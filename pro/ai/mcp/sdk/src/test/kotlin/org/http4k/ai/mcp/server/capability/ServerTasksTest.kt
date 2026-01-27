package org.http4k.ai.mcp.server.capability

import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.equalTo
import com.natpryce.hamkrest.isEmpty
import org.http4k.ai.mcp.Client
import org.http4k.ai.mcp.model.TaskId
import org.http4k.ai.mcp.model.TaskStatus
import org.http4k.ai.mcp.protocol.SessionId
import org.http4k.ai.mcp.protocol.messages.McpTask
import org.http4k.ai.mcp.server.protocol.Session
import org.http4k.core.Method.GET
import org.http4k.core.Request
import org.junit.jupiter.api.Test
import java.time.Instant

class ServerTasksTest {

    private val tasks = ServerTasks()
    private val session1 = Session(SessionId.of("session-1"))
    private val session2 = Session(SessionId.of("session-2"))
    private val testRequest = Request(GET, "/test")

    @Test
    fun `tasks are isolated by session`() {
        val taskId = TaskId.of("task-1")
        val now = Instant.now()

        tasks.update(session1, McpTask.Status.Notification(
            taskId = taskId,
            status = TaskStatus.working,
            createdAt = now,
            lastUpdatedAt = now
        ))

        val session1Tasks = tasks.list(session1, McpTask.List.Request(), Client.Companion.NoOp, testRequest)
        val session2Tasks = tasks.list(session2, McpTask.List.Request(), Client.Companion.NoOp, testRequest)

        assertThat(session1Tasks.tasks.size, equalTo(1))
        assertThat(session1Tasks.tasks[0].taskId, equalTo(taskId))
        assertThat(session2Tasks.tasks, isEmpty)
    }

    @Test
    fun `remove cleans up all tasks for that session`() {
        val taskId1 = TaskId.of("task-1")
        val taskId2 = TaskId.of("task-2")
        val now = Instant.now()

        tasks.update(session1, McpTask.Status.Notification(
            taskId = taskId1,
            status = TaskStatus.working,
            createdAt = now,
            lastUpdatedAt = now
        ))
        tasks.update(session1, McpTask.Status.Notification(
            taskId = taskId2,
            status = TaskStatus.working,
            createdAt = now,
            lastUpdatedAt = now
        ))

        assertThat(tasks.list(session1, McpTask.List.Request(), Client.Companion.NoOp, testRequest).tasks.size, equalTo(2))

        tasks.remove(session1)

        assertThat(tasks.list(session1, McpTask.List.Request(), Client.Companion.NoOp, testRequest).tasks, isEmpty)
    }
}
