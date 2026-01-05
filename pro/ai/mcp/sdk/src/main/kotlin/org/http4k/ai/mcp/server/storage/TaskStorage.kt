package org.http4k.ai.mcp.server.storage

import org.http4k.ai.mcp.model.Cursor
import org.http4k.ai.mcp.model.Task
import org.http4k.ai.mcp.model.TaskId

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
    fun list(cursor: Cursor? = null, limit: Int = 100): TaskPage

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
}
