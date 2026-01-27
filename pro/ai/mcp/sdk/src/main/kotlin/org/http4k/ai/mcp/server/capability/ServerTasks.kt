package org.http4k.ai.mcp.server.capability

import org.http4k.ai.mcp.Client
import org.http4k.ai.mcp.protocol.McpException
import org.http4k.ai.mcp.protocol.messages.McpTask
import org.http4k.ai.mcp.server.protocol.TaskUpdateCallback
import org.http4k.ai.mcp.server.protocol.Tasks
import org.http4k.ai.mcp.server.storage.TaskStorage
import org.http4k.core.Request
import org.http4k.jsonrpc.ErrorMessage.Companion.InvalidParams
import java.util.concurrent.CopyOnWriteArrayList

class ServerTasks(
    private val storage: TaskStorage = TaskStorage.InMemory()
) : Tasks {
    private val callbacks = CopyOnWriteArrayList<TaskUpdateCallback>()

    override fun get(req: McpTask.Get.Request, client: Client, http: Request): McpTask.Get.Response {
        val task = storage.get(req.taskId)
            ?: throw McpException(InvalidParams)
        return McpTask.Get.Response(task)
    }

    override fun result(req: McpTask.Result.Request, client: Client, http: Request): McpTask.Result.Response =
        McpTask.Result.Response(storage.resultFor(req.taskId))

    override fun cancel(req: McpTask.Cancel.Request, client: Client, http: Request): McpTask.Cancel.Response {
        storage.delete(req.taskId)
        return McpTask.Cancel.Response()
    }

    override fun list(req: McpTask.List.Request, client: Client, http: Request): McpTask.List.Response {
        val page = storage.list(req.cursor)
        return McpTask.List.Response(page.tasks, page.nextCursor)
    }

    override fun onUpdate(callback: TaskUpdateCallback) {
        callbacks += callback
    }

    override fun update(notification: McpTask.Status.Notification) {
        val task = notification.toTask()
        storage.store(task)
        callbacks.forEach { it(task, notification._meta) }
    }
}
