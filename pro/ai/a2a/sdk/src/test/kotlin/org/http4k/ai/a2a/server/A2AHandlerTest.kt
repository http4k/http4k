/*
 * Copyright (c) 2025-present http4k Ltd. All rights reserved.
 * Licensed under the http4k Commercial License: https://http4k.org/commercial-license
 */
package org.http4k.ai.a2a.server

import com.natpryce.hamkrest.absent
import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.equalTo
import com.natpryce.hamkrest.isA
import com.natpryce.hamkrest.present
import org.http4k.ai.a2a.MessageHandler
import org.http4k.ai.a2a.MessageResponse
import org.http4k.ai.a2a.model.AgentCard
import org.http4k.ai.a2a.model.ContextId
import org.http4k.ai.a2a.model.Message
import org.http4k.ai.a2a.model.Part
import org.http4k.ai.a2a.model.PushNotificationConfig
import org.http4k.ai.a2a.model.Task
import org.http4k.ai.a2a.model.TaskId
import org.http4k.ai.a2a.model.TaskState
import org.http4k.ai.a2a.model.TaskState.*
import org.http4k.ai.a2a.model.TaskStatus
import org.http4k.ai.a2a.server.storage.PushNotificationConfigStorage
import org.http4k.ai.a2a.server.storage.TaskStorage
import org.http4k.ai.model.Role
import org.http4k.core.Method.POST
import org.http4k.core.Request
import org.http4k.core.Uri
import org.http4k.protocol.A2A
import org.junit.jupiter.api.Test

class A2AHandlerTest {

    private val testCard = AgentCard(name = "test", url = Uri.of("http://test"), version = "1.0")
    private val tasks = TaskStorage.InMemory()
    private val pushNotifications = PushNotificationConfigStorage.InMemory()

    private fun aTask(id: String = "task-1", contextId: String = "ctx-1") = Task(
        id = TaskId.of(id),
        contextId = ContextId.of(contextId),
        status = TaskStatus(state = completed),
        history = listOf(Message(role = Role.User, parts = listOf(Part.Text("hello"))))
    )

    private fun aMessage() = Message(role = Role.User, parts = listOf(Part.Text("hello")))

    private fun taskHandler(vararg states: TaskState): MessageHandler = { request ->
        MessageResponse.Task(states.map { state ->
            Task(
                id = TaskId.of("task-1"),
                contextId = ContextId.of("ctx-1"),
                status = TaskStatus(state = state),
                history = listOf(request.message)
            )
        }.asSequence())
    }

    private fun messageHandler(): MessageHandler = {
        MessageResponse.Message(Message(role = Role.Assistant, parts = listOf(Part.Text("response"))))
    }

    @Test
    fun `send returns task response`() {
        val protocol = A2A(testCard, tasks, pushNotifications, taskHandler(completed))
        val result = protocol.send(aMessage(), Request(POST, "/"))

        assertThat(result, isA<MessageResponse.Task>())
        assertThat((result as MessageResponse.Task).tasks.first().status.state, equalTo(completed))
    }

    @Test
    fun `send returns message response`() {
        val protocol = A2A(testCard, tasks, pushNotifications, messageHandler())
        val result = protocol.send(aMessage(), Request(POST, "/"))

        assertThat(result, isA<MessageResponse.Message>())
    }

    @Test
    fun `send returns streaming task responses`() {
        val protocol = A2A(testCard, tasks, pushNotifications, taskHandler(working, completed))
        val result = protocol.send(aMessage(), Request(POST, "/"))

        val responses = (result as MessageResponse.Task).tasks.toList()
        assertThat(responses.size, equalTo(2))
        assertThat(responses[0].status.state, equalTo(working))
        assertThat(responses[1].status.state, equalTo(completed))
    }

    @Test
    fun `getTask returns stored task`() {
        val protocol = A2A(testCard, tasks, pushNotifications, taskHandler(completed))
        val task = aTask()
        tasks.store(task)

        assertThat(protocol.getTask(task.id), present(equalTo(task)))
    }

    @Test
    fun `getTask returns null for unknown task`() {
        val protocol = A2A(testCard, tasks, pushNotifications, taskHandler(completed))
        assertThat(protocol.getTask(TaskId.of("unknown")), absent())
    }

    @Test
    fun `cancelTask sets state to canceled`() {
        val protocol = A2A(testCard, tasks, pushNotifications, taskHandler(completed))
        tasks.store(aTask())

        val result = protocol.cancelTask(TaskId.of("task-1"))
        assertThat(result!!.status.state, equalTo(canceled))
    }

    @Test
    fun `listTasks returns stored tasks`() {
        val protocol = A2A(testCard, tasks, pushNotifications, taskHandler(completed))
        tasks.store(aTask("t1"))
        tasks.store(aTask("t2"))

        val page = protocol.listTasks()
        assertThat(page.tasks.size, equalTo(2))
        assertThat(page.totalSize, equalTo(2))
    }

    @Test
    fun `push config set and get`() {
        val protocol = A2A(testCard, tasks, pushNotifications, taskHandler(completed))

        val config = protocol.setPushConfig(TaskId.of("task-1"), PushNotificationConfig(url = Uri.of("https://example.com/webhook")))
        assertThat(config.taskId, equalTo(TaskId.of("task-1")))

        val retrieved = protocol.getPushConfig(config.id)
        assertThat(retrieved!!.id, equalTo(config.id))
    }

    @Test
    fun `push config list`() {
        val protocol = A2A(testCard, tasks, pushNotifications, taskHandler(completed))
        val taskId = TaskId.of("task-1")

        protocol.setPushConfig(taskId, PushNotificationConfig(url = Uri.of("https://a.com")))
        protocol.setPushConfig(taskId, PushNotificationConfig(url = Uri.of("https://b.com")))

        assertThat(protocol.listPushConfigs(taskId).size, equalTo(2))
    }

    @Test
    fun `push config delete`() {
        val protocol = A2A(testCard, tasks, pushNotifications, taskHandler(completed))
        val taskId = TaskId.of("task-1")

        val config = protocol.setPushConfig(taskId, PushNotificationConfig(url = Uri.of("https://a.com")))
        assertThat(protocol.deletePushConfig(config.id), present(equalTo(config.id)))
        assertThat(protocol.listPushConfigs(taskId).size, equalTo(0))
    }
}
