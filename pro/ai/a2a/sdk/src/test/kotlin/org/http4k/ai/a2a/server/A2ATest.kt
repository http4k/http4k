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
import org.http4k.ai.a2a.model.MessageId
import org.http4k.ai.a2a.model.Part
import org.http4k.ai.a2a.model.StreamMessage
import java.util.UUID
import org.http4k.ai.a2a.model.PushNotificationConfig
import org.http4k.ai.a2a.model.Task
import org.http4k.ai.a2a.model.TaskId
import org.http4k.ai.a2a.model.TaskState
import org.http4k.ai.a2a.model.TaskState.TASK_STATE_CANCELED
import org.http4k.ai.a2a.model.TaskState.TASK_STATE_COMPLETED
import org.http4k.ai.a2a.model.TaskState.TASK_STATE_WORKING
import org.http4k.ai.a2a.model.TaskStatus
import org.http4k.ai.a2a.model.Version
import org.http4k.ai.a2a.server.storage.PushNotificationConfigStorage
import org.http4k.ai.a2a.server.storage.TaskStorage
import org.http4k.ai.model.Role
import org.http4k.core.Method.POST
import org.http4k.core.Request
import org.http4k.core.Uri
import org.http4k.ai.a2a.protocol.messages.A2AMessage
import org.http4k.ai.a2a.protocol.messages.A2APushNotificationConfig
import org.http4k.ai.a2a.protocol.messages.A2ATask
import org.http4k.ai.a2a.protocol.messages.toWire
import org.http4k.protocol.A2A
import org.junit.jupiter.api.Test

class A2ATest {

    private val testCard = AgentCard(name = "test", url = Uri.of("http://test"), version = Version.of("1.0.0"))
    private val tasks = TaskStorage.InMemory()
    private val pushNotifications = PushNotificationConfigStorage.InMemory()

    private fun aTask(id: String = "task-1", contextId: String = "ctx-1") = Task(
        id = TaskId.of(id),
        contextId = ContextId.of(contextId),
        status = TaskStatus(state = TASK_STATE_COMPLETED),
        history = listOf(Message(messageId = MessageId.of(UUID.randomUUID().toString()), role = Role.User, parts = listOf(Part.Text("hello"))))
    )

    private fun aMessage() = Message(messageId = MessageId.of(UUID.randomUUID().toString()), role = Role.User, parts = listOf(Part.Text("hello")))

    private fun taskHandler(state: TaskState): MessageHandler = { request ->
        MessageResponse.Task(
            Task(
                id = TaskId.of("task-1"),
                contextId = ContextId.of("ctx-1"),
                status = TaskStatus(state = state),
                history = listOf(request.message)
            )
        )
    }

    private fun streamHandler(vararg states: TaskState): MessageHandler = { request ->
        MessageResponse.Stream(states.map { state ->
            StreamMessage.Task(Task(
                id = TaskId.of("task-1"),
                contextId = ContextId.of("ctx-1"),
                status = TaskStatus(state = state),
                history = listOf(request.message)
            ))
        }.asSequence())
    }

    private fun messageHandler(): MessageHandler = {
        MessageResponse.Message(Message(messageId = MessageId.of(UUID.randomUUID().toString()), role = Role.Assistant, parts = listOf(Part.Text("response"))))
    }

    @Test
    fun `send returns task response`() {
        val protocol = A2A(testCard, tasks, pushNotifications, taskHandler(TASK_STATE_COMPLETED))
        val result = protocol.send(A2AMessage.Send.Request.Params(aMessage().toWire()), Request(POST, "/"))

        assertThat(result, isA<MessageResponse.Task>())
        assertThat((result as MessageResponse.Task).task.status.state, equalTo(TASK_STATE_COMPLETED))
    }

    @Test
    fun `send returns message response`() {
        val protocol = A2A(testCard, tasks, pushNotifications, messageHandler())
        val result = protocol.send(A2AMessage.Send.Request.Params(aMessage().toWire()), Request(POST, "/"))

        assertThat(result, isA<MessageResponse.Message>())
    }

    @Test
    fun `stream returns streaming task responses`() {
        val protocol = A2A(testCard, tasks, pushNotifications, streamHandler(TASK_STATE_WORKING, TASK_STATE_COMPLETED))
        val responses = protocol.stream(A2AMessage.Stream.Request.Params(aMessage().toWire()), Request(POST, "/")).toList()
        assertThat(responses.size, equalTo(2))
        assertThat((responses[0] as StreamMessage.Task).task.status.state, equalTo(TASK_STATE_WORKING))
        assertThat((responses[1] as StreamMessage.Task).task.status.state, equalTo(TASK_STATE_COMPLETED))
    }

    @Test
    fun `getTask returns stored task`() {
        val protocol = A2A(testCard, tasks, pushNotifications, taskHandler(TASK_STATE_COMPLETED))
        val task = aTask()
        tasks.store(task)

        assertThat(protocol.getTask(A2ATask.Get.Request.Params(task.id)), present(equalTo(task)))
    }

    @Test
    fun `getTask returns null for unknown task`() {
        val protocol = A2A(testCard, tasks, pushNotifications, taskHandler(TASK_STATE_COMPLETED))
        assertThat(protocol.getTask(A2ATask.Get.Request.Params(TaskId.of("unknown"))), absent())
    }

    @Test
    fun `cancelTask sets state to canceled`() {
        val protocol = A2A(testCard, tasks, pushNotifications, taskHandler(TASK_STATE_COMPLETED))
        tasks.store(aTask())

        val result = protocol.cancelTask(A2ATask.Cancel.Request.Params(TaskId.of("task-1")))
        assertThat(result!!.status.state, equalTo(TASK_STATE_CANCELED))
    }

    @Test
    fun `listTasks returns stored tasks`() {
        val protocol = A2A(testCard, tasks, pushNotifications, taskHandler(TASK_STATE_COMPLETED))
        tasks.store(aTask("t1"))
        tasks.store(aTask("t2"))

        val page = protocol.listTasks(A2ATask.ListTasks.Request.Params())
        assertThat(page.tasks.size, equalTo(2))
        assertThat(page.totalSize, equalTo(2))
    }

    @Test
    fun `push config set and get`() {
        val protocol = A2A(testCard, tasks, pushNotifications, taskHandler(TASK_STATE_COMPLETED))

        val config = protocol.setPushConfig(A2APushNotificationConfig.Set.Request.Params(TaskId.of("task-1"), PushNotificationConfig(url = Uri.of("https://example.com/webhook"))))
        assertThat(config.taskId, equalTo(TaskId.of("task-1")))

        val retrieved = protocol.getPushConfig(A2APushNotificationConfig.Get.Request.Params(TaskId.of("task-1"), config.id))
        assertThat(retrieved!!.id, equalTo(config.id))
    }

    @Test
    fun `push config list`() {
        val protocol = A2A(testCard, tasks, pushNotifications, taskHandler(TASK_STATE_COMPLETED))
        val taskId = TaskId.of("task-1")

        protocol.setPushConfig(A2APushNotificationConfig.Set.Request.Params(taskId, PushNotificationConfig(url = Uri.of("https://a.com"))))
        protocol.setPushConfig(A2APushNotificationConfig.Set.Request.Params(taskId, PushNotificationConfig(url = Uri.of("https://b.com"))))

        assertThat(protocol.listPushConfigs(A2APushNotificationConfig.List.Request.Params(taskId)).size, equalTo(2))
    }

    @Test
    fun `push config delete`() {
        val protocol = A2A(testCard, tasks, pushNotifications, taskHandler(TASK_STATE_COMPLETED))
        val taskId = TaskId.of("task-1")

        val config = protocol.setPushConfig(A2APushNotificationConfig.Set.Request.Params(taskId, PushNotificationConfig(url = Uri.of("https://a.com"))))
        assertThat(protocol.deletePushConfig(A2APushNotificationConfig.Delete.Request.Params(taskId, config.id)), present(equalTo(config.id)))
        assertThat(protocol.listPushConfigs(A2APushNotificationConfig.List.Request.Params(taskId)).size, equalTo(0))
    }
}
