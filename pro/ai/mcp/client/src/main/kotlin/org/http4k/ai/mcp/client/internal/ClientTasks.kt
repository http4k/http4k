/*
 * Copyright (c) 2025-present http4k Ltd. All rights reserved.
 * Licensed under the http4k Commercial License: https://http4k.org/commercial-license
 */
package org.http4k.ai.mcp.client.internal

import dev.forkhandles.result4k.flatMap
import dev.forkhandles.result4k.map
import org.http4k.ai.mcp.McpResult
import org.http4k.ai.mcp.client.McpClient
import org.http4k.ai.mcp.model.McpMessageId
import org.http4k.ai.mcp.model.Meta
import org.http4k.ai.mcp.model.Task
import org.http4k.ai.mcp.model.TaskId
import org.http4k.ai.mcp.protocol.messages.McpRpc
import org.http4k.ai.mcp.protocol.messages.McpTask
import org.http4k.ai.mcp.util.McpNodeType
import java.time.Duration

internal class ClientTasks(
    private val queueFor: (McpMessageId) -> Iterable<McpNodeType>,
    private val tidyUp: (McpMessageId) -> Unit,
    private val sender: McpRpcSender,
    private val id: () -> McpMessageId,
    private val defaultTimeout: Duration,
    private val register: ((McpRpc, McpCallback<*>) -> Any)? = null
) : McpClient.Tasks {

    override fun onUpdate(fn: (Task, Meta) -> Unit) {
        register?.invoke(McpTask.Status, McpCallback(McpTask.Status.Notification.Params::class) { notification, _ ->
            fn(notification.toTask(), notification._meta)
        })
    }

    override fun get(taskId: TaskId, overrideDefaultTimeout: Duration?): McpResult<Task> {
        val messageId = id()
        return sender(
            McpTask.Get.Request(McpTask.Get.Request.Params(taskId), messageId),
            overrideDefaultTimeout ?: defaultTimeout,
            messageId
        )
            .map { reqId -> queueFor(reqId).also { tidyUp(reqId) } }
            .flatMap { it.first().asOrFailure<McpTask.Get.Response.Result>() }
            .map { it.task }
    }

    override fun list(overrideDefaultTimeout: Duration?): McpResult<List<Task>> {
        val messageId = id()
        return sender(
            McpTask.List.Request(McpTask.List.Request.Params(), messageId),
            overrideDefaultTimeout ?: defaultTimeout,
            messageId
        )
            .map { reqId -> queueFor(reqId).also { tidyUp(reqId) } }
            .flatMap { it.first().asOrFailure<McpTask.List.Response.Result>() }
            .map { it.tasks }
    }

    override fun cancel(taskId: TaskId, overrideDefaultTimeout: Duration?): McpResult<Unit> {
        val messageId = id()
        return sender(
            McpTask.Cancel.Request(McpTask.Cancel.Request.Params(taskId), messageId),
            overrideDefaultTimeout ?: defaultTimeout,
            messageId
        )
            .map { reqId -> queueFor(reqId).also { tidyUp(reqId) } }
            .flatMap { it.first().asOrFailure<McpTask.Cancel.Response.Result>() }
            .map { }
    }

    override fun result(taskId: TaskId, overrideDefaultTimeout: Duration?): McpResult<Map<String, Any>> {
        val messageId = id()
        return sender(
            McpTask.Result.Request(McpTask.Result.Request.Params(taskId), messageId),
            overrideDefaultTimeout ?: defaultTimeout,
            messageId
        )
            .map { reqId -> queueFor(reqId).also { tidyUp(reqId) } }
            .flatMap { it.first().asOrFailure<McpTask.Result.Response.ResponseResult>() }
            .map { it.result }
    }

    override fun update(task: Task, meta: Meta, overrideDefaultTimeout: Duration?) {
        val messageId = id()
        sender(
            McpTask.Status.Notification(McpTask.Status.Notification.Params(task, meta), messageId),
            overrideDefaultTimeout ?: defaultTimeout,
            messageId
        )
    }
}
