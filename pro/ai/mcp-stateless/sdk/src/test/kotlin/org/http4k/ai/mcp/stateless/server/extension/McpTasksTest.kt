/*
 * Copyright (c) 2025-present http4k Ltd. All rights reserved.
 * Licensed under the http4k Commercial License: https://http4k.org/commercial-license
 */
package org.http4k.ai.mcp.stateless.server.extension

import org.http4k.ai.mcp.stateless.ElicitationRequest
import org.http4k.ai.mcp.stateless.ToolResponse
import org.http4k.ai.mcp.stateless.model.ElicitationAction.accept
import org.http4k.ai.mcp.stateless.model.Meta
import org.http4k.ai.mcp.stateless.model.TaskId
import org.http4k.ai.mcp.stateless.model.Tool
import org.http4k.ai.mcp.stateless.model.TtlMs
import org.http4k.ai.mcp.stateless.protocol.ClientCapabilities
import org.http4k.ai.mcp.stateless.protocol.ProtocolVersion.Companion.LATEST_VERSION
import org.http4k.ai.mcp.stateless.protocol.ServerMetaData
import org.http4k.ai.mcp.stateless.protocol.messages.McpElicitation
import org.http4k.ai.mcp.stateless.protocol.messages.McpTask
import org.http4k.ai.mcp.stateless.protocol.messages.McpTool
import org.http4k.ai.mcp.stateless.server.capability.then
import org.http4k.ai.mcp.stateless.server.capability.tools
import org.http4k.ai.mcp.stateless.server.protocol.McpProtocol
import org.http4k.ai.mcp.stateless.util.McpJson
import org.http4k.ai.model.ToolName
import org.http4k.core.ContentType.Companion.APPLICATION_JSON
import org.http4k.core.Method.POST
import org.http4k.core.Request
import org.http4k.lens.stateless.MetaKey
import org.http4k.lens.stateless.clientCapabilities
import org.http4k.lens.stateless.protocolVersion
import org.http4k.routing.stateless.bind
import org.http4k.testing.Approver
import org.http4k.testing.JsonApprovalTest
import org.http4k.testing.assertApproved
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import java.time.Instant
import java.util.concurrent.CountDownLatch

@ExtendWith(JsonApprovalTest::class)
class McpTasksTest {

    private var now = Instant.parse("2026-01-01T12:00:00Z")
    private val taskId = TaskId.of("a-fixed-task-id")
    private val blocked = CountDownLatch(1)
    private val finished = CountDownLatch(1)
    private var settled = CountDownLatch(1)

    private val tasks = McpTasks(clock = { now }, newTaskId = { taskId })

    private val server = McpProtocol(
        ServerMetaData("tasks-server", "1.0.0"),
        tools = tools(
            tasks.asTask(ttlMs = TtlMs.of(60000), pollIntervalMs = 500)
                .then(Tool("slow", "takes a while") bind {
                    blocked.await()
                    finished.countDown()
                    ToolResponse.Ok("all done")
                }),
            tasks.asTask().then(Tool("quick", "returns at once") bind { ToolResponse.Ok("immediate") }),
            tasks.asTask().then(Tool("asks", "needs an answer") bind { req ->
                settled.countDown()
                when (req.inputResponses["name"]) {
                    null -> ToolResponse.InputRequired(
                        mapOf("name" to ElicitationRequest.Form("Your name?", McpJson.obj())),
                        requestState = "state-from-the-handler"
                    )

                    else -> ToolResponse.Ok("hello there")
                }
            })
        ),
        extensions = listOf(tasks)
    )

    @AfterEach
    fun release() = blocked.countDown()

    @Test
    fun `a tasked tool answers with a handle before the work finishes`(approver: Approver) {
        approver.assertApproved(server(call("slow")).bodyString(), APPLICATION_JSON)
    }

    @Test
    fun `polling a task still running reports it as working`(approver: Approver) {
        server(call("slow"))

        approver.assertApproved(server(get(taskId)).bodyString(), APPLICATION_JSON)
    }

    @Test
    fun `polling a finished task yields the tool result`(approver: Approver) {
        server(call("slow"))
        blocked.countDown()
        finished.await()

        approver.assertApproved(server(get(taskId)).bodyString(), APPLICATION_JSON)
    }

    @Test
    fun `a client which has not declared the extension is never given a handle`(approver: Approver) {
        approver.assertApproved(server(call("quick", declaresTasks = false)).bodyString(), APPLICATION_JSON)
    }

    @Test
    fun `cancelling a running task`(approver: Approver) {
        server(call("slow"))

        approver.assertApproved(server(cancel(taskId)).bodyString(), APPLICATION_JSON)
    }

    @Test
    fun `a cancelled task discards the result its tool eventually produces`(approver: Approver) {
        server(call("slow"))
        server(cancel(taskId))
        blocked.countDown()
        finished.await()

        approver.assertApproved(server(get(taskId)).bodyString(), APPLICATION_JSON)
    }

    @Test
    fun `a task which outlives its ttl is reported as failed`(approver: Approver) {
        server(call("slow"))
        now = now.plusMillis(60001)

        approver.assertApproved(server(get(taskId)).bodyString(), APPLICATION_JSON)
    }

    @Test
    fun `polling without declaring the extension is rejected`(approver: Approver) {
        server(call("slow"))

        approver.assertApproved(server(get(taskId, declaresTasks = false)).bodyString(), APPLICATION_JSON)
    }

    @Test
    fun `a task awaiting input surfaces what it needs`(approver: Approver) {
        server(call("asks"))

        approver.assertApproved(awaitStatus(taskId, "input_required"), APPLICATION_JSON)
    }

    @Test
    fun `answering via tasks-update re-runs the tool to completion`(approver: Approver) {
        server(call("asks"))
        awaitStatus(taskId, "input_required")

        server(update(taskId))

        approver.assertApproved(awaitStatus(taskId, "completed"), APPLICATION_JSON)
    }

    @Test
    fun `answering a task which is not awaiting input is rejected`(approver: Approver) {
        server(call("slow"))

        approver.assertApproved(server(update(taskId)).bodyString(), APPLICATION_JSON)
    }

    @Test
    fun `polling an unknown task is rejected`(approver: Approver) {
        approver.assertApproved(server(get(TaskId.of("never-existed"))).bodyString(), APPLICATION_JSON)
    }

    // the store is written by the background run, so poll for the outcome rather than the tool entering
    private fun awaitStatus(id: TaskId, status: String): String {
        repeat(100) {
            val body = server(get(id)).bodyString()
            if (body.contains(status)) return body
            Thread.sleep(20)
        }
        error("task never reached $status")
    }

    private fun meta(declaresTasks: Boolean = true) = MetaKey.clientCapabilities().toLens()(
        ClientCapabilities().let {
            if (declaresTasks) it.withExtensions(TASKS_EXTENSION to emptyMap<String, Any>()) else it
        },
        MetaKey.protocolVersion().toLens()(LATEST_VERSION, Meta.default)
    )

    private fun call(name: String, declaresTasks: Boolean = true) = post(
        "tools/call", name,
        McpTool.Call.Request(McpTool.Call.Request.Params(ToolName.of(name), _meta = meta(declaresTasks)), "1")
    )

    private fun get(id: TaskId, declaresTasks: Boolean = true) = post(
        "tasks/get", id.value,
        McpTask.Get.Request(McpTask.Get.Request.Params(id, _meta = meta(declaresTasks)), "1")
    )

    private fun cancel(id: TaskId) = post(
        "tasks/cancel", id.value,
        McpTask.Cancel.Request(McpTask.Cancel.Request.Params(id, _meta = meta()), "1")
    )

    private fun update(id: TaskId) = post(
        "tasks/update", id.value,
        McpTask.Update.Request(
            McpTask.Update.Request.Params(
                id,
                mapOf("name" to McpElicitation.Result(accept, McpJson.obj("input" to McpJson.string("Luca")))),
                _meta = meta()
            ), "1"
        )
    )

    private fun post(method: String, name: String, message: Any) = Request(POST, "/mcp")
        .header("mcp-protocol-version", LATEST_VERSION.value)
        .header("mcp-method", method)
        .header("mcp-name", name)
        .body(McpJson.asFormatString(message))
}
