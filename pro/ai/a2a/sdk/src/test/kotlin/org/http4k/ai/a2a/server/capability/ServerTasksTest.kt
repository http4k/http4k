package org.http4k.ai.a2a.server.capability

import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.equalTo
import org.http4k.ai.a2a.model.ContextId
import org.http4k.ai.a2a.model.Task
import org.http4k.ai.a2a.model.TaskId
import org.http4k.ai.a2a.model.TaskState
import org.http4k.ai.a2a.model.TaskStatus
import org.http4k.ai.a2a.protocol.messages.A2ATask
import org.junit.jupiter.api.Test

class ServerTasksTest {

    private val tasks = ServerTasks()

    private fun aTask(
        id: String = "task-1",
        contextId: String = "ctx-1",
        state: TaskState = TaskState.working
    ) = Task(
        id = TaskId.of(id),
        contextId = ContextId.of(contextId),
        status = TaskStatus(state = state)
    )

    @Test
    fun `list returns empty page when no tasks exist`() {
        val response = tasks.list(A2ATask.List.Request())

        assertThat(response.tasks, equalTo(emptyList()))
        assertThat(response.totalSize, equalTo(0))
        assertThat(response.nextPageToken, equalTo(""))
    }

    @Test
    fun `list returns all tasks when no filters applied`() {
        tasks.store(aTask("task-1"))
        tasks.store(aTask("task-2"))

        val response = tasks.list(A2ATask.List.Request())

        assertThat(response.tasks.size, equalTo(2))
        assertThat(response.totalSize, equalTo(2))
    }

    @Test
    fun `list filters by contextId`() {
        tasks.store(aTask("task-1", contextId = "ctx-1"))
        tasks.store(aTask("task-2", contextId = "ctx-2"))
        tasks.store(aTask("task-3", contextId = "ctx-1"))

        val response = tasks.list(A2ATask.List.Request(contextId = ContextId.of("ctx-1")))

        assertThat(response.tasks.size, equalTo(2))
        assertThat(response.tasks.all { it.contextId == ContextId.of("ctx-1") }, equalTo(true))
    }

    @Test
    fun `list filters by status`() {
        tasks.store(aTask("task-1", state = TaskState.working))
        tasks.store(aTask("task-2", state = TaskState.completed))
        tasks.store(aTask("task-3", state = TaskState.working))

        val response = tasks.list(A2ATask.List.Request(status = TaskState.working))

        assertThat(response.tasks.size, equalTo(2))
        assertThat(response.tasks.all { it.status.state == TaskState.working }, equalTo(true))
    }

    @Test
    fun `list respects pageSize limit`() {
        tasks.store(aTask("task-1"))
        tasks.store(aTask("task-2"))
        tasks.store(aTask("task-3"))

        val response = tasks.list(A2ATask.List.Request(pageSize = 2))

        assertThat(response.tasks.size, equalTo(2))
        assertThat(response.totalSize, equalTo(3))
        assertThat(response.pageSize, equalTo(2))
        assertThat(response.nextPageToken.isNotEmpty(), equalTo(true))
    }

    @Test
    fun `list paginates using pageToken`() {
        tasks.store(aTask("task-1"))
        tasks.store(aTask("task-2"))
        tasks.store(aTask("task-3"))

        val firstResponse = tasks.list(A2ATask.List.Request(pageSize = 2))
        val secondResponse = tasks.list(A2ATask.List.Request(pageSize = 2, pageToken = firstResponse.nextPageToken))

        assertThat(secondResponse.tasks.size, equalTo(1))
        assertThat(secondResponse.nextPageToken, equalTo(""))
        assertThat(secondResponse.totalSize, equalTo(3))
    }

    @Test
    fun `list combines filters`() {
        tasks.store(aTask("task-1", contextId = "ctx-1", state = TaskState.working))
        tasks.store(aTask("task-2", contextId = "ctx-1", state = TaskState.completed))
        tasks.store(aTask("task-3", contextId = "ctx-2", state = TaskState.working))

        val response = tasks.list(
            A2ATask.List.Request(
                contextId = ContextId.of("ctx-1"),
                status = TaskState.working
            )
        )

        assertThat(response.tasks.size, equalTo(1))
        assertThat(response.tasks[0].id, equalTo(TaskId.of("task-1")))
    }

    @Test
    fun `get returns response with task when found`() {
        val task = aTask("task-1")
        tasks.store(task)

        val response = tasks.get(A2ATask.Get.Request(id = TaskId.of("task-1")))

        assertThat(response?.task, equalTo(task))
    }

    @Test
    fun `get returns null when task not found`() {
        val response = tasks.get(A2ATask.Get.Request(id = TaskId.of("non-existent")))

        assertThat(response, equalTo(null))
    }

    @Test
    fun `cancel returns response with cancelled task when found`() {
        tasks.store(aTask("task-1", state = TaskState.working))

        val response = tasks.cancel(A2ATask.Cancel.Request(id = TaskId.of("task-1")))

        assertThat(response?.task?.status?.state, equalTo(TaskState.canceled))
    }

    @Test
    fun `cancel returns null when task not found`() {
        val response = tasks.cancel(A2ATask.Cancel.Request(id = TaskId.of("non-existent")))

        assertThat(response, equalTo(null))
    }

    @Test
    fun `list returns all tasks without pagination when pageSize is null`() {
        tasks.store(aTask("task-1"))
        tasks.store(aTask("task-2"))
        tasks.store(aTask("task-3"))

        val response = tasks.list(A2ATask.List.Request(pageSize = null))

        assertThat(response.tasks.size, equalTo(3))
        assertThat(response.totalSize, equalTo(3))
        assertThat(response.pageSize, equalTo(null))
        assertThat(response.nextPageToken, equalTo(""))
    }
}
