package org.http4k.ai.mcp.client.internal

import dev.forkhandles.result4k.flatMap
import dev.forkhandles.result4k.map
import org.http4k.ai.mcp.client.McpClient
import org.http4k.ai.mcp.model.McpMessageId
import org.http4k.ai.mcp.model.Meta
import org.http4k.ai.mcp.model.Task
import org.http4k.ai.mcp.model.TaskId
import org.http4k.ai.mcp.protocol.messages.McpRpc
import org.http4k.ai.mcp.protocol.messages.McpTask
import org.http4k.ai.mcp.util.McpNodeType
import java.time.Duration
import kotlin.random.Random

internal class ClientTasks(
    private val queueFor: (McpMessageId) -> Iterable<McpNodeType>,
    private val tidyUp: (McpMessageId) -> Unit,
    private val sender: McpRpcSender,
    private val random: Random,
    private val defaultTimeout: Duration,
    private val register: ((McpRpc, McpCallback<*>) -> Any)? = null
) : McpClient.Tasks {

    override fun onUpdate(fn: (Task, Meta) -> Unit) {
        register?.invoke(McpTask.Status, McpCallback(McpTask.Status.Notification::class) { notification, _ ->
            fn(notification.toTask(), notification._meta)
        })
    }

    override fun get(taskId: TaskId, overrideDefaultTimeout: Duration?) = sender(
        McpTask.Get, McpTask.Get.Request(taskId),
        overrideDefaultTimeout ?: defaultTimeout,
        McpMessageId.random(random)
    )
        .map { reqId -> queueFor(reqId).also { tidyUp(reqId) } }
        .flatMap { it.first().asOrFailure<McpTask.Get.Response>() }
        .map { it.task }

    override fun list(overrideDefaultTimeout: Duration?) = sender(
        McpTask.List, McpTask.List.Request(),
        overrideDefaultTimeout ?: defaultTimeout,
        McpMessageId.random(random)
    )
        .map { reqId -> queueFor(reqId).also { tidyUp(reqId) } }
        .flatMap { it.first().asOrFailure<McpTask.List.Response>() }
        .map { it.tasks }

    override fun cancel(taskId: TaskId, overrideDefaultTimeout: Duration?) = sender(
        McpTask.Cancel, McpTask.Cancel.Request(taskId),
        overrideDefaultTimeout ?: defaultTimeout,
        McpMessageId.random(random)
    )
        .map { reqId -> queueFor(reqId).also { tidyUp(reqId) } }
        .flatMap { it.first().asOrFailure<McpTask.Cancel.Response>() }
        .map { }

    override fun result(taskId: TaskId, overrideDefaultTimeout: Duration?) = sender(
        McpTask.Result, McpTask.Result.Request(taskId),
        overrideDefaultTimeout ?: defaultTimeout,
        McpMessageId.random(random)
    )
        .map { reqId -> queueFor(reqId).also { tidyUp(reqId) } }
        .flatMap { it.first().asOrFailure<McpTask.Result.Response>() }
        .map { it.result }

    override fun update(task: Task, meta: Meta, overrideDefaultTimeout: Duration?) {
        sender(
            McpTask.Status, McpTask.Status.Notification(task, meta),
            overrideDefaultTimeout ?: defaultTimeout,
            McpMessageId.random(random)
        )
    }
}
