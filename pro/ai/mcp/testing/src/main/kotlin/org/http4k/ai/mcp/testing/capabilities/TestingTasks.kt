package org.http4k.ai.mcp.testing.capabilities

import dev.forkhandles.result4k.map
import org.http4k.ai.mcp.client.McpClient
import org.http4k.ai.mcp.model.Meta
import org.http4k.ai.mcp.model.Task
import org.http4k.ai.mcp.model.TaskId
import org.http4k.ai.mcp.protocol.messages.McpTask
import org.http4k.ai.mcp.testing.TestMcpSender
import org.http4k.ai.mcp.testing.nextEvent
import org.http4k.ai.mcp.testing.nextNotification
import java.time.Duration

class TestingTasks(
    private val sender: TestMcpSender
) : McpClient.Tasks {
    private val notifications = mutableListOf<(Task, Meta) -> Unit>()

    override fun onUpdate(fn: (Task, Meta) -> Unit) {
        notifications += fn
    }

    fun expectNotification() =
        sender.stream().nextNotification<McpTask.Status.Notification>(McpTask.Status)
            .also {
                it.let { notification ->
                    notifications.forEach { fn ->
                        fn(
                            notification.toTask(),
                            notification._meta
                        )
                    }
                }
            }

    override fun get(taskId: TaskId, overrideDefaultTimeout: Duration?) =
        sender(McpTask.Get, McpTask.Get.Request(taskId)).first()
            .nextEvent<Task, McpTask.Get.Response> { task }
            .map { it.second }

    override fun list(overrideDefaultTimeout: Duration?) =
        sender(McpTask.List, McpTask.List.Request()).first()
            .nextEvent<List<Task>, McpTask.List.Response> { tasks }
            .map { it.second }

    override fun cancel(taskId: TaskId, overrideDefaultTimeout: Duration?) =
        sender(McpTask.Cancel, McpTask.Cancel.Request(taskId)).first()
            .nextEvent<Unit, McpTask.Cancel.Response> { }
            .map { it.second }

    override fun result(taskId: TaskId, overrideDefaultTimeout: Duration?) =
        sender(McpTask.Result, McpTask.Result.Request(taskId)).first()
            .nextEvent<Map<String, Any>?, McpTask.Result.Response> { result }
            .map { it.second }

    override fun update(task: Task, meta: Meta, overrideDefaultTimeout: Duration?) {
        sender(McpTask.Status, McpTask.Status.Notification(task, meta)).toList()
    }
}
