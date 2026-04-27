/*
 * Copyright (c) 2025-present http4k Ltd. All rights reserved.
 * Licensed under the http4k Commercial License: https://http4k.org/commercial-license
 */
package org.http4k.ai.a2a.server.capability

import org.http4k.ai.a2a.model.Task
import org.http4k.ai.a2a.model.TaskState
import org.http4k.ai.a2a.model.TaskStatus
import org.http4k.ai.a2a.protocol.messages.A2ATask
import org.http4k.ai.a2a.server.protocol.Tasks
import org.http4k.ai.a2a.server.storage.TaskStorage

fun tasks(storage: TaskStorage = TaskStorage.InMemory()): Tasks = ServerTasks(storage)

private class ServerTasks(private val storage: TaskStorage) : Tasks {

    override fun get(params: A2ATask.Get.Request.Params) =
        storage.get(params.id)?.let { A2ATask.Get.Response.Result(it) }

    override fun store(task: Task) = storage.store(task)

    override fun cancel(params: A2ATask.Cancel.Request.Params): A2ATask.Cancel.Response.Result? {
        val task = storage.get(params.id) ?: return null
        val cancelledTask = task.copy(status = TaskStatus(state = TaskState.canceled))
        storage.store(cancelledTask)
        return A2ATask.Cancel.Response.Result(cancelledTask)
    }

    override fun list(params: A2ATask.List.Request.Params): A2ATask.List.Response.Result {
        val page = storage.list(params.contextId, params.status, params.pageSize, params.pageToken)
        return A2ATask.List.Response.Result(page.tasks, page.nextPageToken, params.pageSize, page.totalSize)
    }
}
