package org.http4k.ai.mcp.server.storage

import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.equalTo
import com.natpryce.hamkrest.isEmpty
import org.http4k.ai.mcp.model.Task
import org.http4k.ai.mcp.model.TaskId
import org.http4k.ai.mcp.model.TaskStatus
import org.http4k.ai.mcp.protocol.SessionId
import org.http4k.ai.mcp.server.protocol.Session
import org.junit.jupiter.api.Test
import java.time.Instant

class InMemoryTaskStorageTest {

    private val storage = TaskStorage.InMemory()
    private val session1 = Session(SessionId.of("session-1"))
    private val session2 = Session(SessionId.of("session-2"))
    private val now = Instant.now()

    @Test
    fun `tasks are scoped by session - list returns only tasks for that session`() {
        val task1 = task(TaskId.of("task-1"))
        val task2 = task(TaskId.of("task-2"))

        storage.store(session1, task1)
        storage.store(session2, task2)

        val session1Tasks = storage.list(session1).tasks
        val session2Tasks = storage.list(session2).tasks

        assertThat(session1Tasks, equalTo(listOf(task1)))
        assertThat(session2Tasks, equalTo(listOf(task2)))
    }

    @Test
    fun `get retrieves task only for matching session`() {
        val task = task(TaskId.of("task-1"))

        storage.store(session1, task)

        assertThat(storage.get(session1, task.taskId), equalTo(task))
        assertThat(storage.get(session2, task.taskId), equalTo(null))
    }

    @Test
    fun `delete removes task only for matching session`() {
        val task = task(TaskId.of("task-1"))

        storage.store(session1, task)
        storage.delete(session2, task.taskId)

        assertThat(storage.get(session1, task.taskId), equalTo(task))

        storage.delete(session1, task.taskId)

        assertThat(storage.get(session1, task.taskId), equalTo(null))
    }

    @Test
    fun `storeResult and resultFor are scoped by session`() {
        val taskId = TaskId.of("task-1")
        val task = task(taskId)
        val result = mapOf("key" to "value")

        storage.store(session1, task)
        storage.storeResult(session1, taskId, result)

        assertThat(storage.resultFor(session1, taskId), equalTo(result))
        assertThat(storage.resultFor(session2, taskId), equalTo(null))
    }

    @Test
    fun `remove cleans up all tasks and results for that session`() {
        val task1 = task(TaskId.of("task-1"))
        val task2 = task(TaskId.of("task-2"))
        val result = mapOf("key" to "value")

        storage.store(session1, task1)
        storage.store(session1, task2)
        storage.storeResult(session1, task1.taskId, result)

        storage.remove(session1)

        assertThat(storage.list(session1).tasks, isEmpty)
        assertThat(storage.get(session1, task1.taskId), equalTo(null))
        assertThat(storage.resultFor(session1, task1.taskId), equalTo(null))
    }

    private fun task(taskId: TaskId) = Task(
        taskId = taskId,
        status = TaskStatus.working,
        createdAt = now,
        lastUpdatedAt = now
    )
}
