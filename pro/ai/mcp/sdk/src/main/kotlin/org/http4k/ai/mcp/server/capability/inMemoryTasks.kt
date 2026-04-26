/*
 * Copyright (c) 2025-present http4k Ltd. All rights reserved.
 * Licensed under the http4k Commercial License: https://http4k.org/commercial-license
 */
package org.http4k.ai.mcp.server.capability

import org.http4k.ai.mcp.Client
import org.http4k.ai.mcp.model.TaskId
import org.http4k.ai.mcp.protocol.McpException
import org.http4k.ai.mcp.protocol.messages.McpTask
import org.http4k.ai.mcp.server.protocol.Session
import org.http4k.ai.mcp.server.protocol.TaskUpdateCallback
import org.http4k.ai.mcp.server.protocol.Tasks
import org.http4k.ai.mcp.server.storage.TaskStorage
import org.http4k.core.Request
import org.http4k.jsonrpc.ErrorMessage.Companion.InvalidParams
import java.util.concurrent.CopyOnWriteArrayList

fun tasks(storage: TaskStorage = TaskStorage.InMemory()): Tasks = InMemoryTasks(storage)

private class InMemoryTasks(private val storage: TaskStorage = TaskStorage.InMemory()) : Tasks {
    private val callbacks = CopyOnWriteArrayList<TaskUpdateCallback>()

    override fun get(session: Session, req: McpTask.Get.Request.Params, client: Client, http: Request): McpTask.Get.Response.Result {
        val task = storage.get(session, req.taskId)
            ?: throw McpException(InvalidParams)
        return McpTask.Get.Response.Result(task)
    }

    override fun result(session: Session, req: McpTask.Result.Request.Params, client: Client, http: Request): McpTask.Result.Response.ResponseResult =
        McpTask.Result.Response.ResponseResult(storage.resultFor(session, req.taskId))

    override fun cancel(session: Session, req: McpTask.Cancel.Request.Params, client: Client, http: Request): McpTask.Cancel.Response.Result {
        storage.delete(session, req.taskId)
        return McpTask.Cancel.Response.Result()
    }

    override fun list(session: Session, req: McpTask.List.Request.Params, client: Client, http: Request): McpTask.List.Response.Result {
        val page = storage.list(session, req.cursor)
        return McpTask.List.Response.Result(page.tasks, page.nextCursor)
    }

    override fun onUpdate(callback: TaskUpdateCallback) {
        callbacks += callback
    }

    override fun update(session: Session, notification: McpTask.Status.Notification.Params) {
        val task = notification.toTask()
        storage.store(session, task)
        callbacks.forEach { it(task, notification._meta) }
    }

    override fun storeResult(session: Session, taskId: TaskId, result: Map<String, Any>) {
        storage.storeResult(session, taskId, result)
    }

    override fun remove(session: Session) {
        storage.remove(session)
    }
}
