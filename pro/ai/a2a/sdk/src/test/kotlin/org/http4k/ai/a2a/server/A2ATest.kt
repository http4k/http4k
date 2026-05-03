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
import org.http4k.ai.a2a.model.AgentCard
import org.http4k.ai.a2a.model.Artifact
import org.http4k.ai.a2a.model.ArtifactId
import org.http4k.ai.a2a.model.ContextId
import org.http4k.ai.a2a.model.Message
import org.http4k.ai.a2a.model.MessageId
import org.http4k.ai.a2a.model.ResponseStream
import org.http4k.ai.a2a.model.Part
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
import org.http4k.ai.a2a.protocol.messages.TaskConfiguration
import org.http4k.connect.model.MimeType
import org.http4k.ai.a2a.server.storage.PushNotificationConfigStorage
import org.http4k.ai.a2a.server.storage.TaskStorage
import org.http4k.ai.model.Role
import org.http4k.core.Method.GET
import org.http4k.core.Method.POST
import org.http4k.core.Request
import org.http4k.core.Uri
import org.http4k.ai.a2a.protocol.messages.A2AMessage
import org.http4k.ai.a2a.protocol.messages.A2APushNotificationConfig
import org.http4k.ai.a2a.protocol.messages.A2ATask
import org.http4k.ai.a2a.server.TaskSubscriptions
import org.http4k.ai.a2a.server.storage.withSubscriptions
import org.http4k.protocol.A2A
import org.http4k.sse.Sse
import org.http4k.sse.SseMessage
import org.junit.jupiter.api.Test
import java.util.concurrent.CopyOnWriteArrayList

class A2ATest {

    private val testCard = AgentCard(name = "test", url = Uri.of("http://test"), version = Version.of("1.0.0"), description = "test agent")
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
        Task(
            id = TaskId.of("task-1"),
            contextId = ContextId.of("ctx-1"),
            status = TaskStatus(state = state),
            history = listOf(request.message)
        )
    }

    private fun streamHandler(vararg states: TaskState): MessageHandler = { request ->
        ResponseStream(states.map { state ->
            Task(
                id = TaskId.of("task-1"),
                contextId = ContextId.of("ctx-1"),
                status = TaskStatus(state = state),
                history = listOf(request.message)
            )
        }.asSequence())
    }

    private fun messageHandler(): MessageHandler = {
        Message(messageId = MessageId.of(UUID.randomUUID().toString()), role = Role.Assistant, parts = listOf(Part.Text("response")))
    }

    @Test
    fun `send returns task response`() {
        val protocol = A2A(testCard, tasks, pushNotifications, handler = taskHandler(TASK_STATE_COMPLETED))
        val result = protocol.send(A2AMessage.Send.Request.Params(aMessage()), Request(POST, "/"))

        assertThat(result, isA<Task>())
        assertThat((result as Task).status.state, equalTo(TASK_STATE_COMPLETED))
    }

    @Test
    fun `send returns message response`() {
        val protocol = A2A(testCard, tasks, pushNotifications, handler = messageHandler())
        val result = protocol.send(A2AMessage.Send.Request.Params(aMessage()), Request(POST, "/"))

        assertThat(result, isA<Message>())
    }

    @Test
    fun `stream returns streaming task responses`() {
        val protocol = A2A(testCard, tasks, pushNotifications, handler = streamHandler(TASK_STATE_WORKING, TASK_STATE_COMPLETED))
        val responses = protocol.stream(A2AMessage.Stream.Request.Params(aMessage()), Request(POST, "/")).toList()
        assertThat(responses.size, equalTo(2))
        assertThat((responses[0] as Task).status.state, equalTo(TASK_STATE_WORKING))
        assertThat((responses[1] as Task).status.state, equalTo(TASK_STATE_COMPLETED))
    }

    @Test
    fun `getTask returns stored task`() {
        val protocol = A2A(testCard, tasks, pushNotifications, handler = taskHandler(TASK_STATE_COMPLETED))
        val task = aTask()
        tasks.store(task)

        assertThat(protocol.getTask(A2ATask.Get.Request.Params(task.id)), present(equalTo(task)))
    }

    @Test
    fun `getTask returns null for unknown task`() {
        val protocol = A2A(testCard, tasks, pushNotifications, handler = taskHandler(TASK_STATE_COMPLETED))
        assertThat(protocol.getTask(A2ATask.Get.Request.Params(TaskId.of("unknown"))), absent())
    }

    @Test
    fun `cancelTask sets state to canceled`() {
        val protocol = A2A(testCard, tasks, pushNotifications, handler = taskHandler(TASK_STATE_COMPLETED))
        tasks.store(aTask())

        val result = protocol.cancelTask(A2ATask.Cancel.Request.Params(TaskId.of("task-1")))
        assertThat(result!!.status.state, equalTo(TASK_STATE_CANCELED))
    }

    @Test
    fun `listTasks returns stored tasks`() {
        val protocol = A2A(testCard, tasks, pushNotifications, handler = taskHandler(TASK_STATE_COMPLETED))
        tasks.store(aTask("t1"))
        tasks.store(aTask("t2"))

        val page = protocol.listTasks(A2ATask.ListTasks.Request.Params())
        assertThat(page.tasks.size, equalTo(2))
        assertThat(page.totalSize, equalTo(2))
    }

    @Test
    fun `send passes configuration and metadata to handler`() {
        val config = TaskConfiguration(acceptedOutputModes = listOf(MimeType.of("text/plain")), historyLength = 5)
        val metadata = mapOf("key" to "value")
        var receivedConfig: TaskConfiguration? = null
        var receivedMetadata: Map<String, Any>? = null

        val handler: MessageHandler = { request ->
            receivedConfig = request.configuration
            receivedMetadata = request.metadata
            Message(messageId = MessageId.of(UUID.randomUUID().toString()), role = Role.Assistant, parts = listOf(Part.Text("ok")))
        }

        val protocol = A2A(testCard, tasks, pushNotifications, handler = handler)
        protocol.send(A2AMessage.Send.Request.Params(aMessage(), configuration = config, metadata = metadata), Request(POST, "/"))

        assertThat(receivedConfig, equalTo(config))
        assertThat(receivedMetadata, equalTo(metadata))
    }

    @Test
    fun `getTask with historyLength trims history`() {
        val protocol = A2A(testCard, tasks, pushNotifications, handler = taskHandler(TASK_STATE_COMPLETED))
        val messages = (1..5).map { Message(messageId = MessageId.of("msg-$it"), role = Role.User, parts = listOf(Part.Text("msg $it"))) }
        val task = Task(id = TaskId.of("task-1"), contextId = ContextId.of("ctx-1"), status = TaskStatus(state = TASK_STATE_COMPLETED), history = messages)
        tasks.store(task)

        val result = protocol.getTask(A2ATask.Get.Request.Params(TaskId.of("task-1"), historyLength = 2))!!
        assertThat(result.history!!.size, equalTo(2))
        assertThat(result.history!!.first().messageId, equalTo(MessageId.of("msg-4")))
    }

    @Test
    fun `listTasks with historyLength trims history`() {
        val protocol = A2A(testCard, tasks, pushNotifications, handler = taskHandler(TASK_STATE_COMPLETED))
        val messages = (1..3).map { Message(messageId = MessageId.of("msg-$it"), role = Role.User, parts = listOf(Part.Text("msg $it"))) }
        tasks.store(Task(id = TaskId.of("t1"), contextId = ContextId.of("ctx-1"), status = TaskStatus(state = TASK_STATE_COMPLETED), history = messages))

        val page = protocol.listTasks(A2ATask.ListTasks.Request.Params(historyLength = 1))
        assertThat(page.tasks.first().history!!.size, equalTo(1))
    }

    @Test
    fun `listTasks with includeArtifacts false strips artifacts`() {
        val protocol = A2A(testCard, tasks, pushNotifications, handler = taskHandler(TASK_STATE_COMPLETED))
        tasks.store(Task(
            id = TaskId.of("t1"), contextId = ContextId.of("ctx-1"),
            status = TaskStatus(state = TASK_STATE_COMPLETED),
            artifacts = listOf(Artifact(artifactId = ArtifactId.of("a1"), parts = listOf(Part.Text("artifact"))))
        ))

        val page = protocol.listTasks(A2ATask.ListTasks.Request.Params(includeArtifacts = false))
        assertThat(page.tasks.first().artifacts, absent())
    }

    @Test
    fun `push config set and get`() {
        val protocol = A2A(testCard, tasks, pushNotifications, handler = taskHandler(TASK_STATE_COMPLETED))

        val config = protocol.setPushConfig(A2APushNotificationConfig.Set.Request.Params(TaskId.of("task-1"), PushNotificationConfig(url = Uri.of("https://example.com/webhook"))))
        assertThat(config.taskId, equalTo(TaskId.of("task-1")))

        val retrieved = protocol.getPushConfig(A2APushNotificationConfig.Get.Request.Params(TaskId.of("task-1"), config.id))
        assertThat(retrieved!!.id, equalTo(config.id))
    }

    @Test
    fun `push config list`() {
        val protocol = A2A(testCard, tasks, pushNotifications, handler = taskHandler(TASK_STATE_COMPLETED))
        val taskId = TaskId.of("task-1")

        protocol.setPushConfig(A2APushNotificationConfig.Set.Request.Params(taskId, PushNotificationConfig(url = Uri.of("https://a.com"))))
        protocol.setPushConfig(A2APushNotificationConfig.Set.Request.Params(taskId, PushNotificationConfig(url = Uri.of("https://b.com"))))

        assertThat(protocol.listPushConfigs(A2APushNotificationConfig.List.Request.Params(taskId)).configs.size, equalTo(2))
    }

    @Test
    fun `push config delete`() {
        val protocol = A2A(testCard, tasks, pushNotifications, handler = taskHandler(TASK_STATE_COMPLETED))
        val taskId = TaskId.of("task-1")

        val config = protocol.setPushConfig(A2APushNotificationConfig.Set.Request.Params(taskId, PushNotificationConfig(url = Uri.of("https://a.com"))))
        assertThat(protocol.deletePushConfig(A2APushNotificationConfig.Delete.Request.Params(taskId, config.id)), present(equalTo(config.id)))
        assertThat(protocol.listPushConfigs(A2APushNotificationConfig.List.Request.Params(taskId)).configs.size, equalTo(0))
    }

    @Test
    fun `subscribe sends current task and returns it`() {
        val subscriptions = TaskSubscriptions.InMemory()
        val subscribingTasks = tasks.withSubscriptions(subscriptions)
        val protocol = A2A(testCard, subscribingTasks, pushNotifications, subscriptions, handler = taskHandler(TASK_STATE_COMPLETED))
        val task = aTask()
        subscribingTasks.store(task)

        val sse = RecordingSse()
        val result = protocol.subscribe(TaskId.of("task-1"), sse)

        assertThat(result, present())
        assertThat(result!!.id, equalTo(TaskId.of("task-1")))
        assertThat(sse.messages.size, equalTo(1))
    }

    @Test
    fun `subscribe returns null for unknown task`() {
        val protocol = A2A(testCard, tasks, pushNotifications, handler = taskHandler(TASK_STATE_COMPLETED))
        val sse = RecordingSse()
        assertThat(protocol.subscribe(TaskId.of("unknown"), sse), absent())
    }

    @Test
    fun `subscribe receives updates when task stored`() {
        val subscriptions = TaskSubscriptions.InMemory()
        val subscribingTasks = tasks.withSubscriptions(subscriptions)
        val protocol = A2A(testCard, subscribingTasks, pushNotifications, subscriptions, handler = taskHandler(TASK_STATE_COMPLETED))
        val task = aTask()
        subscribingTasks.store(task)

        val sse = RecordingSse()
        protocol.subscribe(TaskId.of("task-1"), sse)

        val updated = task.copy(status = TaskStatus(state = TASK_STATE_WORKING))
        subscribingTasks.store(updated)

        assertThat(sse.messages.size, equalTo(2))
    }

    private class RecordingSse : Sse {
        val messages = CopyOnWriteArrayList<SseMessage>()
        private val closeHandlers = mutableListOf<() -> Unit>()

        override val connectRequest = Request(GET, "/")
        override fun send(message: SseMessage) = apply { messages.add(message) }
        override fun close() { closeHandlers.forEach { it() } }
        override fun onClose(fn: () -> Unit) = apply { closeHandlers.add(fn) }
    }
}
