package org.http4k.ai.mcp.server.protocol

import org.http4k.ai.mcp.Client
import org.http4k.ai.mcp.model.Meta
import org.http4k.ai.mcp.model.Task
import org.http4k.ai.mcp.model.TaskId
import org.http4k.ai.mcp.protocol.messages.McpTask
import org.http4k.core.Request

/**
 * Handles protocol traffic for server task management.
 * All operations are scoped by session.
 */
interface Tasks {
    fun get(session: Session, req: McpTask.Get.Request, client: Client, http: Request): McpTask.Get.Response

    fun result(session: Session, req: McpTask.Result.Request, client: Client, http: Request): McpTask.Result.Response

    fun cancel(session: Session, req: McpTask.Cancel.Request, client: Client, http: Request): McpTask.Cancel.Response

    fun list(session: Session, req: McpTask.List.Request, client: Client, http: Request): McpTask.List.Response

    fun onUpdate(callback: TaskUpdateCallback)

    fun update(session: Session, notification: McpTask.Status.Notification)

    fun storeResult(session: Session, taskId: TaskId, result: Map<String, Any>)

    fun remove(session: Session)
}

fun interface TaskUpdateCallback {
    operator fun invoke(task: Task, meta: Meta)
}
