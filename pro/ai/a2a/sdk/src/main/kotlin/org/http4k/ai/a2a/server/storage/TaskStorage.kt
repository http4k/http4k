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
import org.http4k.ai.a2a.model.Tenant
import java.util.concurrent.ConcurrentHashMap

private fun Task.trimHistory(historyLength: Int?): Task {
    val h = history ?: return this
    return if (historyLength != null) copy(history = h.takeLast(historyLength)) else this
}

interface TaskStorage {
    fun store(task: Task, tenant: Tenant? = null)
    fun get(taskId: TaskId, historyLength: Int? = null, tenant: Tenant? = null): Task?
    fun delete(taskId: TaskId, tenant: Tenant? = null)

    fun cancel(taskId: TaskId, tenant: Tenant? = null): Task? {
        val task = get(taskId, tenant = tenant) ?: return null
        val cancelled = task.copy(status = TaskStatus(state = TaskState.TASK_STATE_CANCELED))
        store(cancelled, tenant)
        return cancelled
    }

    fun list(
        contextId: ContextId? = null,
        status: TaskState? = null,
        pageSize: Int? = null,
        pageToken: String? = null,
        historyLength: Int? = null,
        includeArtifacts: Boolean? = null,
        tenant: Tenant? = null
    ): TaskPage

    companion object {
        fun InMemory() = object : TaskStorage {
            private val NO_TENANT = Tenant.of("__no_tenant__")
            private val tenants = ConcurrentHashMap<Tenant, ConcurrentHashMap<TaskId, Task>>()

            private fun tasksFor(tenant: Tenant?) = tenants.getOrPut(tenant ?: NO_TENANT) { ConcurrentHashMap() }

            override fun store(task: Task, tenant: Tenant?) {
                tasksFor(tenant)[task.id] = task
            }

            override fun get(taskId: TaskId, historyLength: Int?, tenant: Tenant?): Task? =
                tasksFor(tenant)[taskId]?.trimHistory(historyLength)

            override fun delete(taskId: TaskId, tenant: Tenant?) {
                tasksFor(tenant).remove(taskId)
            }

            override fun list(
                contextId: ContextId?,
                status: TaskState?,
                pageSize: Int?,
                pageToken: String?,
                historyLength: Int?,
                includeArtifacts: Boolean?,
                tenant: Tenant?
            ): TaskPage {
                val filtered = tasksFor(tenant).values
                    .filter { contextId == null || it.contextId == contextId }
                    .filter { status == null || it.status.state == status }
                    .sortedBy { it.id.value }
                    .map { it.trimHistory(historyLength) }
                    .map { if (includeArtifacts == false) it.copy(artifacts = null) else it }

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
