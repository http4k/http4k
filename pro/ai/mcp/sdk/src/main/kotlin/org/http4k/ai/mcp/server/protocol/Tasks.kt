/*
 * Copyright (c) 2025-present http4k Ltd. All rights reserved.
 * Licensed under the http4k Commercial License: https://http4k.org/commercial-license
 */
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
    fun get(session: Session, req: McpTask.Get.Request.Params, client: Client, http: Request): McpTask.Get.Response.Result

    fun result(session: Session, req: McpTask.Result.Request.Params, client: Client, http: Request): McpTask.Result.Response.ResponseResult

    fun cancel(session: Session, req: McpTask.Cancel.Request.Params, client: Client, http: Request): McpTask.Cancel.Response.Result

    fun list(session: Session, req: McpTask.List.Request.Params, client: Client, http: Request): McpTask.List.Response.Result

    fun onUpdate(callback: TaskUpdateCallback)

    fun update(session: Session, notification: McpTask.Status.Notification.Params)

    fun storeResult(session: Session, taskId: TaskId, result: Map<String, Any>)

    fun remove(session: Session)
}

fun interface TaskUpdateCallback {
    operator fun invoke(task: Task, meta: Meta)
}
