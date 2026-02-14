package org.http4k.ai.a2a.server.storage

import org.http4k.ai.a2a.model.ContextId
import org.http4k.ai.a2a.model.Task
import org.http4k.ai.a2a.model.TaskId
import java.util.concurrent.ConcurrentHashMap

/**
 * Pluggable storage interface for A2A tasks.
 * Implementations can provide in-memory, database, or other persistence strategies.
 */
interface TaskStorage {
    fun store(task: Task)
    fun get(taskId: TaskId): Task?
    fun delete(taskId: TaskId)
    fun list(): List<Task>
    fun listByContext(contextId: ContextId): List<Task>

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

            override fun list(): List<Task> = tasks.values.toList()

            override fun listByContext(contextId: ContextId): List<Task> =
                tasks.values.filter { it.contextId == contextId }
        }
    }
}
