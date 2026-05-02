/*
 * Copyright (c) 2025-present http4k Ltd. All rights reserved.
 * Licensed under the http4k Commercial License: https://http4k.org/commercial-license
 */
package org.http4k.ai.a2a.server.storage

import com.natpryce.hamkrest.absent
import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.equalTo
import com.natpryce.hamkrest.present
import org.http4k.ai.a2a.model.ContextId
import org.http4k.ai.a2a.model.Task
import org.http4k.ai.a2a.model.TaskId
import org.http4k.ai.a2a.model.TaskState
import org.http4k.ai.a2a.model.TaskStatus
import org.junit.jupiter.api.Test

class TaskStorageTest {

    private val storage = TaskStorage.InMemory()

    private fun aTask(
        id: String = "task-1",
        contextId: String = "ctx-1",
        state: TaskState = TaskState.TASK_STATE_COMPLETED
    ) = Task(
        id = TaskId.of(id),
        contextId = ContextId.of(contextId),
        status = TaskStatus(state = state)
    )

    @Test
    fun `store and get`() {
        val task = aTask()
        storage.store(task)
        assertThat(storage.get(task.id), present(equalTo(task)))
    }

    @Test
    fun `get returns null for unknown task`() {
        assertThat(storage.get(TaskId.of("unknown")), absent())
    }

    @Test
    fun `delete removes task`() {
        val task = aTask()
        storage.store(task)
        storage.delete(task.id)
        assertThat(storage.get(task.id), absent())
    }

    @Test
    fun `cancel sets state to canceled`() {
        val task = aTask(state = TaskState.TASK_STATE_WORKING)
        storage.store(task)

        val cancelled = storage.cancel(task.id)

        assertThat(cancelled!!.status.state, equalTo(TaskState.TASK_STATE_CANCELED))
        assertThat(storage.get(task.id)!!.status.state, equalTo(TaskState.TASK_STATE_CANCELED))
    }

    @Test
    fun `cancel returns null for unknown task`() {
        assertThat(storage.cancel(TaskId.of("unknown")), absent())
    }

    @Test
    fun `list returns all tasks`() {
        storage.store(aTask("t1"))
        storage.store(aTask("t2"))

        val page = storage.list()
        assertThat(page.tasks.size, equalTo(2))
        assertThat(page.totalSize, equalTo(2))
    }

    @Test
    fun `list filters by contextId`() {
        storage.store(aTask("t1", contextId = "ctx-a"))
        storage.store(aTask("t2", contextId = "ctx-b"))
        storage.store(aTask("t3", contextId = "ctx-a"))

        val page = storage.list(contextId = ContextId.of("ctx-a"))
        assertThat(page.tasks.size, equalTo(2))
        assertThat(page.totalSize, equalTo(2))
    }

    @Test
    fun `list filters by status`() {
        storage.store(aTask("t1", state = TaskState.TASK_STATE_WORKING))
        storage.store(aTask("t2", state = TaskState.TASK_STATE_COMPLETED))
        storage.store(aTask("t3", state = TaskState.TASK_STATE_WORKING))

        val page = storage.list(status = TaskState.TASK_STATE_WORKING)
        assertThat(page.tasks.size, equalTo(2))
    }

    @Test
    fun `list paginates`() {
        storage.store(aTask("t1"))
        storage.store(aTask("t2"))
        storage.store(aTask("t3"))

        val page1 = storage.list(pageSize = 2)
        assertThat(page1.tasks.size, equalTo(2))
        assertThat(page1.totalSize, equalTo(3))
        assertThat(page1.nextPageToken.isNotEmpty(), equalTo(true))

        val page2 = storage.list(pageSize = 2, pageToken = page1.nextPageToken)
        assertThat(page2.tasks.size, equalTo(1))
        assertThat(page2.nextPageToken, equalTo(""))
    }

    @Test
    fun `list combines filters and pagination`() {
        storage.store(aTask("t1", contextId = "ctx-a", state = TaskState.TASK_STATE_WORKING))
        storage.store(aTask("t2", contextId = "ctx-a", state = TaskState.TASK_STATE_COMPLETED))
        storage.store(aTask("t3", contextId = "ctx-b", state = TaskState.TASK_STATE_WORKING))
        storage.store(aTask("t4", contextId = "ctx-a", state = TaskState.TASK_STATE_WORKING))

        val page = storage.list(contextId = ContextId.of("ctx-a"), status = TaskState.TASK_STATE_WORKING, pageSize = 1)
        assertThat(page.tasks.size, equalTo(1))
        assertThat(page.totalSize, equalTo(2))
        assertThat(page.nextPageToken.isNotEmpty(), equalTo(true))
    }
}
