package org.http4k.ai.mcp.server.capability

import org.http4k.ai.mcp.Client
import org.http4k.ai.mcp.protocol.McpException
import org.http4k.ai.mcp.protocol.messages.McpTask
import org.http4k.ai.mcp.server.protocol.Tasks
import org.http4k.ai.mcp.server.storage.TaskStorage
import org.http4k.core.Request
import org.http4k.jsonrpc.ErrorMessage.Companion.InvalidParams

class ServerTasks(
    private val storage: TaskStorage = TaskStorage.InMemory()
) : Tasks {
    override fun get(req: McpTask.Get.Request, client: Client, http: Request): McpTask.Get.Response {
        val task = storage.get(req.taskId)
            ?: throw McpException(InvalidParams)
        return McpTask.Get.Response(task)
    }

    override fun result(req: McpTask.Result.Request, client: Client, http: Request): McpTask.Result.Response {
        val result = storage.getResult(req.taskId)
        return McpTask.Result.Response(result)
    }

    override fun cancel(req: McpTask.Cancel.Request, client: Client, http: Request): McpTask.Cancel.Response {
        storage.delete(req.taskId)
        return McpTask.Cancel.Response()
    }

    override fun list(req: McpTask.List.Request, client: Client, http: Request): McpTask.List.Response {
        val page = storage.list(req.cursor)
        return McpTask.List.Response(page.tasks, page.nextCursor)
    }
}
