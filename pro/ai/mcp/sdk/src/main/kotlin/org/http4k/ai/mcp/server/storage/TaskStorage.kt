package org.http4k.ai.mcp.server.storage

import org.http4k.ai.mcp.model.Cursor
import org.http4k.ai.mcp.model.Task
import org.http4k.ai.mcp.model.TaskId
import java.time.Clock
import java.time.Instant
import java.util.concurrent.ConcurrentHashMap

/**
 * Pluggable storage interface for MCP tasks.
 * Implementations can provide in-memory, database, or other persistence strategies.
 */
interface TaskStorage {
    /**
     * Store a new task or update an existing task.
     */
    fun store(task: Task)

    /**
     * Retrieve a task by ID. Returns null if not found or expired.
     */
    fun get(taskId: TaskId): Task?

    /**
     * Delete a task by ID.
     */
    fun delete(taskId: TaskId)

    /**
     * List all tasks with optional pagination.
     */
    fun list(cursor: Cursor? = null, limit: Int): TaskPage

    /**
     * Store the result payload for a completed task.
     */
    fun storeResult(taskId: TaskId, result: Any?)

    /**
     * Retrieve the result payload for a task.
     */
    fun getResult(taskId: TaskId): Any?

    data class TaskPage(
        val tasks: List<Task>,
        val nextCursor: Cursor? = null
    )

    companion object {
        /**
         * In-memory implementation of TaskStorage with TTL support.
         * Tasks are automatically removed when their TTL expires.
         */
        fun InMemory(clock: Clock = Clock.systemUTC()) = object : TaskStorage {
            private val tasks = ConcurrentHashMap<TaskId, Task>()
            private val results = ConcurrentHashMap<TaskId, Any?>()

            override fun store(task: Task) {
                tasks[task.taskId] = task
            }

            override fun get(taskId: TaskId): Task? {
                val task = tasks[taskId] ?: return null
                return if (isExpired(task)) {
                    delete(taskId)
                    null
                } else {
                    task
                }
            }

            override fun delete(taskId: TaskId) {
                tasks.remove(taskId)
                results.remove(taskId)
            }

            override fun list(cursor: Cursor?, limit: Int): TaskPage {
                cleanupExpired()

                val offset = cursor?.let { it.toIntOrNull() ?: 0 } ?: 0
                val allTasks = tasks.values.sortedBy { it.createdAt }
                val page = allTasks.drop(offset).take(limit)
                val nextCursor = if (allTasks.size > offset + limit) {
                    (offset + limit).toString()
                } else null

                return TaskPage(page, nextCursor)
            }

            override fun storeResult(taskId: TaskId, result: Any?) {
                results[taskId] = result
            }

            override fun getResult(taskId: TaskId) = get(taskId)?.taskId

            private fun isExpired(task: Task): Boolean {
                val ttl = task.ttl ?: return false
                val expiresAt = task.createdAt.plusSeconds(ttl.value.toLong())
                return Instant.now(clock).isAfter(expiresAt)
            }

            private fun cleanupExpired() {
                tasks.values.filter { isExpired(it) }.forEach { delete(it.taskId) }
            }
        }
    }
}
