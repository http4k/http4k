/*
 * Copyright (c) 2025-present http4k Ltd. All rights reserved.
 * Licensed under the http4k Commercial License: https://http4k.org/commercial-license
 */
package org.http4k.ai.mcp.stateless.server.extension

import org.http4k.ai.mcp.stateless.ElicitationResponse
import org.http4k.ai.mcp.stateless.ToolFilter
import org.http4k.ai.mcp.stateless.ToolHandler
import org.http4k.ai.mcp.stateless.ToolRequest
import org.http4k.ai.mcp.stateless.ToolResponse
import org.http4k.ai.mcp.stateless.model.Meta
import org.http4k.ai.mcp.stateless.model.ResultType
import org.http4k.ai.mcp.stateless.model.Task
import org.http4k.ai.mcp.stateless.model.TaskId
import org.http4k.ai.mcp.stateless.model.TaskStatus.cancelled
import org.http4k.ai.mcp.stateless.model.TaskStatus.completed
import org.http4k.ai.mcp.stateless.model.TaskStatus.failed
import org.http4k.ai.mcp.stateless.model.TaskStatus.input_required
import org.http4k.ai.mcp.stateless.model.TaskStatus.working
import org.http4k.ai.mcp.stateless.model.TtlMs
import org.http4k.ai.mcp.stateless.protocol.McpRpcMethod.Companion.of
import org.http4k.ai.mcp.stateless.protocol.messages.McpElicitation
import org.http4k.ai.mcp.stateless.protocol.messages.McpJsonRpcErrorResponse
import org.http4k.ai.mcp.stateless.protocol.messages.McpJsonRpcRequest
import org.http4k.ai.mcp.stateless.protocol.messages.McpTask
import org.http4k.ai.mcp.stateless.protocol.messages.McpTool
import org.http4k.ai.mcp.stateless.protocol.messages.MissingRequiredClientCapabilityError
import org.http4k.ai.mcp.stateless.server.capability.clientCapabilities
import org.http4k.ai.mcp.stateless.server.capability.toElicitationResponses
import org.http4k.ai.mcp.stateless.server.capability.toWireRequests
import org.http4k.ai.mcp.stateless.server.protocol.McpRequest
import org.http4k.ai.mcp.stateless.server.protocol.McpResponse
import org.http4k.ai.mcp.stateless.server.protocol.McpResponse.Accepted
import org.http4k.ai.mcp.stateless.server.protocol.McpResponse.Ok
import org.http4k.ai.mcp.stateless.server.protocol.McpServerExtension
import org.http4k.ai.mcp.stateless.util.McpJson
import org.http4k.ai.mcp.stateless.util.McpNodeType
import org.http4k.jsonrpc.ErrorMessage
import org.http4k.jsonrpc.ErrorMessage.Companion.InternalError
import org.http4k.jsonrpc.ErrorMessage.Companion.InvalidParams
import java.time.Instant
import java.util.concurrent.ConcurrentHashMap

const val TASKS_EXTENSION = "io.modelcontextprotocol/tasks"

data class TaskRecord(
    val task: Task,
    val result: McpNodeType? = null,
    val error: McpNodeType? = null,
    val inputRequests: Map<String, McpElicitation.Create>? = null,
    val requestState: String? = null,
    val resume: ((Map<String, ElicitationResponse>) -> Unit)? = null,
)

class McpTasks(
    private val store: MutableMap<TaskId, TaskRecord> = ConcurrentHashMap(),
    private val clock: () -> Instant = Instant::now,
    private val newTaskId: () -> TaskId = TaskId::random,
) : McpServerExtension {

    override val name = TASKS_EXTENSION
    override val config = emptyMap<String, Any>()
    override val methods = setOf(of("tasks/get"), of("tasks/update"), of("tasks/cancel"))

    fun asTask(ttlMs: TtlMs? = null, pollIntervalMs: Long? = null) = ToolFilter { next ->
        { req -> if (req.declaresTasks()) start(req, next, ttlMs, pollIntervalMs) else next(req) }
    }

    override fun invoke(mcp: McpRequest) = when (val message = mcp.message) {
        is McpTask.Get.Request -> declared(message) { respond(message.params.taskId, message.id) }
        is McpTask.Cancel.Request -> declared(message) { cancel(message.params.taskId, message.id) }
        is McpTask.Update.Request -> declared(message) { answer(message.params, message.id) }
        else -> Accepted
    }

    private fun declared(message: McpJsonRpcRequest, fn: () -> McpResponse) =
        when (message.params?._meta?.declaresTasks()) {
            true -> fn()

            else -> Ok(
                McpJsonRpcErrorResponse(
                    message.id,
                    MissingRequiredClientCapabilityError(requiredExtensions = listOf(TASKS_EXTENSION))
                )
            )
        }

    private fun cancel(taskId: TaskId, id: Any?): McpResponse {
        update(taskId, cancelled) { it }
        return store[taskId]?.let { Ok(McpTask.Cancel.Response(it.asResult(), id)) } ?: notFound(id)
    }

    private fun start(req: ToolRequest, next: ToolHandler, ttlMs: TtlMs?, pollIntervalMs: Long?): ToolResponse {
        val now = clock()
        val task = Task(newTaskId(), working, now, now, ttlMs = ttlMs, pollIntervalMs = pollIntervalMs)
        store[task.taskId] = TaskRecord(task)
        resume(task.taskId, req, next, emptyMap())

        return ToolResponse.Task(task)
    }

    private fun resume(
        taskId: TaskId, req: ToolRequest, next: ToolHandler, answers: Map<String, ElicitationResponse>
    ) {
        Thread.ofVirtual().start {
            runCatching { next(req.copy(inputResponses = answers)) }
                .fold({ settle(taskId, it, req, next, answers) }, { fail(taskId, it) })
        }
    }

    private fun settle(
        taskId: TaskId, outcome: ToolResponse, req: ToolRequest, next: ToolHandler,
        answers: Map<String, ElicitationResponse>
    ) = when (outcome) {
        is ToolResponse.InputRequired -> update(taskId, input_required) {
            it.copy(
                inputRequests = outcome.inputRequests.toWireRequests(req.meta.clientCapabilities()),
                requestState = outcome.requestState,
                resume = { more -> resume(taskId, req, next, answers + more) }
            )
        }

        else -> complete(taskId, outcome)
    }

    // the handler re-runs from the top with the answers supplied - MRTR's re-entrancy contract
    private fun answer(params: McpTask.Update.Request.Params, id: Any?): McpResponse {
        val record = store[params.taskId] ?: return notFound(id)
        val resume = record.resume ?: return notFound(id)

        store[params.taskId] = record.copy(
            task = record.task.copy(status = working, lastUpdatedAt = clock()),
            inputRequests = null, resume = null
        )
        resume(params.inputResponses.toElicitationResponses())

        return store[params.taskId]?.let { Ok(McpTask.Update.Response(it.asResult(), id)) } ?: notFound(id)
    }

    private fun complete(taskId: TaskId, outcome: ToolResponse) = update(taskId, completed) {
        it.copy(result = McpJson.asJsonObject(outcome.asToolResult()))
    }

    private fun fail(taskId: TaskId, cause: Throwable) = update(taskId, failed) {
        it.copy(error = McpJson.asJsonObject(ErrorMessage(InternalError.code, cause.messageOrType())(McpJson)))
    }

    private fun update(taskId: TaskId, status: org.http4k.ai.mcp.stateless.model.TaskStatus, fn: (TaskRecord) -> TaskRecord) {
        store.computeIfPresent(taskId) { _, record ->
            when {
                record.task.status.isTerminal -> record
                else -> fn(record.copy(task = record.task.copy(status = status, lastUpdatedAt = clock())))
            }
        }
    }

    private fun respond(taskId: TaskId, id: Any?) = store[taskId]
        ?.let { Ok(McpTask.Get.Response(it.expireIfStale().asResult(), id)) }
        ?: notFound(id)

    private fun notFound(id: Any?) = Ok(McpJsonRpcErrorResponse(id, InvalidParams))

    // a server MAY fail a task once its ttl elapses - done lazily on read, so nothing has to sweep
    private fun TaskRecord.expireIfStale() = when {
        task.status.isTerminal -> this

        task.ttlMs?.let { clock().isAfter(task.createdAt.plusMillis(it.value)) } != true -> this

        else -> copy(task = task.copy(status = failed, lastUpdatedAt = clock()))
            .also { store[task.taskId] = it }
    }

    private fun TaskRecord.asResult() = McpTask.Result(
        taskId = task.taskId,
        status = task.status,
        statusMessage = task.statusMessage,
        createdAt = task.createdAt,
        lastUpdatedAt = task.lastUpdatedAt,
        ttlMs = task.ttlMs,
        pollIntervalMs = task.pollIntervalMs,
        result = result,
        error = error,
        inputRequests = inputRequests,
        requestState = requestState,
        resultType = if (task.status == input_required) ResultType.input_required else ResultType.complete
    )
}

private fun ToolResponse.asToolResult() = when (this) {
    is ToolResponse.Ok -> McpTool.Call.Response.Result(content, structuredContent, false, _meta = meta)
    is ToolResponse.Error -> McpTool.Call.Response.Result(content, structuredContent, true, _meta = meta)
    else -> McpTool.Call.Response.Result(isError = true, _meta = meta)
}

private fun Throwable.messageOrType() = message ?: this::class.simpleName.orEmpty()

private fun ToolRequest.declaresTasks() = meta.declaresTasks()

private fun Meta.declaresTasks() = clientCapabilities()?.extensions?.containsKey(TASKS_EXTENSION) == true
