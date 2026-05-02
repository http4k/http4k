/*
 * Copyright (c) 2025-present http4k Ltd. All rights reserved.
 * Licensed under the http4k Commercial License: https://http4k.org/commercial-license
 */
package org.http4k.ai.a2a.server.storage

import org.http4k.ai.a2a.model.ContextId
import org.http4k.ai.a2a.model.Task
import org.http4k.ai.a2a.model.TaskId
import org.http4k.ai.a2a.model.TaskPage
import org.http4k.ai.a2a.model.TaskState
import org.http4k.ai.a2a.model.TaskStatus
import java.util.concurrent.ConcurrentHashMap

interface TaskStorage {
    fun store(task: Task)
    fun get(taskId: TaskId): Task?
    fun delete(taskId: TaskId)

    fun cancel(taskId: TaskId): Task? {
        val task = get(taskId) ?: return null
        val cancelled = task.copy(status = TaskStatus(state = TaskState.canceled))
        store(cancelled)
        return cancelled
    }
    fun list(
        contextId: ContextId? = null,
        status: TaskState? = null,
        pageSize: Int? = null,
        pageToken: String? = null
    ): TaskPage

    companion object {
        fun InMemory() = object : TaskStorage {
            private val tasks = ConcurrentHashMap<TaskId, Task>()

            override fun store(task: Task) {
                tasks[task.id] = task
            }

            override fun get(taskId: TaskId): Task? = tasks[taskId]

            override fun delete(taskId: TaskId) {
                tasks.remove(taskId)
            }

            override fun list(
                contextId: ContextId?,
                status: TaskState?,
                pageSize: Int?,
                pageToken: String?
            ): TaskPage {
                val filtered = tasks.values
                    .filter { contextId == null || it.contextId == contextId }
                    .filter { status == null || it.status.state == status }
                    .sortedBy { it.id.value }

                val totalSize = filtered.size
                val (pageTasks, nextToken) = if (pageSize != null) {
                    val startIndex = pageToken?.toIntOrNull() ?: 0
                    val endIndex = minOf(startIndex + pageSize, totalSize)
                    filtered.subList(startIndex, endIndex) to if (endIndex < totalSize) endIndex.toString() else ""
                } else {
                    filtered to ""
                }

                return TaskPage(pageTasks, nextToken, totalSize)
            }
        }
    }
}
