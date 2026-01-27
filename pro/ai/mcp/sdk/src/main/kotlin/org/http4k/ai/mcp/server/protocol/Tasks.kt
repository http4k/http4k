package org.http4k.ai.mcp.server.protocol

import org.http4k.ai.mcp.Client
import org.http4k.ai.mcp.model.Meta
import org.http4k.ai.mcp.model.Task
import org.http4k.ai.mcp.protocol.messages.McpTask
import org.http4k.core.Request

/**
 * Handles protocol traffic for server task management.
 */
interface Tasks {
    fun get(req: McpTask.Get.Request, client: Client, http: Request): McpTask.Get.Response

    fun result(req: McpTask.Result.Request, client: Client, http: Request): McpTask.Result.Response

    fun cancel(req: McpTask.Cancel.Request, client: Client, http: Request): McpTask.Cancel.Response

    fun list(req: McpTask.List.Request, client: Client, http: Request): McpTask.List.Response

    fun onUpdate(callback: TaskUpdateCallback)

    fun update(notification: McpTask.Status.Notification)
}

fun interface TaskUpdateCallback {
    operator fun invoke(task: Task, meta: Meta)
}
