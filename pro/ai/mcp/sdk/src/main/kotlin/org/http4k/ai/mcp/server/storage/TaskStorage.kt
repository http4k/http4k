package org.http4k.ai.mcp.server.storage

import org.http4k.ai.mcp.model.Cursor
import org.http4k.ai.mcp.model.Task
import org.http4k.ai.mcp.model.TaskId
import org.http4k.ai.mcp.protocol.SessionId
import org.http4k.ai.mcp.server.protocol.Session
import java.time.Clock
import java.time.Instant
import java.util.concurrent.ConcurrentHashMap

/**
 * Pluggable storage interface for MCP tasks.
 * Implementations can provide in-memory, database, or other persistence strategies.
 * All operations are scoped by session - tasks are isolated between sessions.
 */
interface TaskStorage {
    /**
     * Store a new task or update an existing task for a session.
     */
    fun store(session: Session, task: Task)

    /**
     * Retrieve a task by ID for a session. Returns null if not found, expired, or belongs to different session.
     */
    fun get(session: Session, taskId: TaskId): Task?

    /**
     * Delete a task by ID for a session.
     */
    fun delete(session: Session, taskId: TaskId)

    /**
     * List all tasks for a session with optional pagination.
     */
    fun list(session: Session, cursor: Cursor? = null): TaskPage

    /**
     * Store the result payload for a completed task in a session.
     */
    fun storeResult(session: Session, taskId: TaskId, result: Map<String, Any>)

    /**
     * Retrieve the result payload for a task in a session.
     */
    fun resultFor(session: Session, taskId: TaskId): Map<String, Any>?

    /**
     * Remove all tasks and results for a session. Called when session closes.
     */
    fun remove(session: Session)

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
            private val tasks = ConcurrentHashMap<Pair<SessionId, TaskId>, Task>()
            private val results = ConcurrentHashMap<Pair<SessionId, TaskId>, Map<String, Any>>()

            override fun store(session: Session, task: Task) {
                tasks[session.id to task.taskId] = task
            }

            override fun get(session: Session, taskId: TaskId): Task? {
                val key = session.id to taskId
                val task = tasks[key] ?: return null
                return if (isExpired(task)) {
                    delete(session, taskId)
                    null
                } else {
                    task
                }
            }

            override fun delete(session: Session, taskId: TaskId) {
                val key = session.id to taskId
                tasks.remove(key)
                results.remove(key)
            }

            override fun list(session: Session, cursor: Cursor?): TaskPage {
                cleanupExpired()

                val offset = cursor?.let { it.toIntOrNull() ?: 0 } ?: 0
                val sessionTasks = tasks.entries
                    .filter { it.key.first == session.id }
                    .map { it.value }
                    .sortedBy { it.createdAt }
                return TaskPage(sessionTasks.drop(offset), if (sessionTasks.size > offset) offset.toString() else null)
            }

            override fun storeResult(session: Session, taskId: TaskId, result: Map<String, Any>) {
                results[session.id to taskId] = result
            }

            override fun resultFor(session: Session, taskId: TaskId) = results[session.id to taskId]

            override fun remove(session: Session) {
                tasks.keys.filter { it.first == session.id }.forEach { tasks.remove(it) }
                results.keys.filter { it.first == session.id }.forEach { results.remove(it) }
            }

            private fun isExpired(task: Task): Boolean {
                val ttl = task.ttl ?: return false
                val expiresAt = task.createdAt.plusSeconds(ttl.value.toLong())
                return Instant.now(clock).isAfter(expiresAt)
            }

            private fun cleanupExpired() {
                tasks.entries.filter { isExpired(it.value) }.forEach {
                    tasks.remove(it.key)
                    results.remove(it.key)
                }
            }
        }
    }
}
