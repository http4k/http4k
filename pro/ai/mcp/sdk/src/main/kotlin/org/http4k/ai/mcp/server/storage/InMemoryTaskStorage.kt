package org.http4k.ai.mcp.server.storage

import org.http4k.ai.mcp.model.Cursor
import org.http4k.ai.mcp.model.Task
import org.http4k.ai.mcp.model.TaskId
import java.time.Clock
import java.time.Instant
import java.util.concurrent.ConcurrentHashMap

/**
 * In-memory implementation of TaskStorage with TTL support.
 * Tasks are automatically removed when their TTL expires.
 */
class InMemoryTaskStorage(
    private val clock: Clock = Clock.systemUTC()
) : TaskStorage {
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

    override fun list(cursor: Cursor?, limit: Int): TaskStorage.TaskPage {
        cleanupExpired()

        val offset = cursor?.let { it.toIntOrNull() ?: 0 } ?: 0
        val allTasks = tasks.values.sortedBy { it.createdAt }
        val page = allTasks.drop(offset).take(limit)
        val nextCursor = if (allTasks.size > offset + limit) {
            (offset + limit).toString()
        } else null

        return TaskStorage.TaskPage(page, nextCursor)
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
