/*
 * Copyright (c) 2025-present http4k Ltd. All rights reserved.
 * Licensed under the http4k Commercial License: https://http4k.org/commercial-license
 */
package org.http4k.ai.a2a.client

import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.equalTo
import dev.forkhandles.result4k.valueOrNull
import org.http4k.ai.a2a.model.AgentCapabilities
import org.http4k.ai.a2a.model.AgentCard
import org.http4k.ai.a2a.model.ContextId
import org.http4k.ai.a2a.model.Message
import org.http4k.ai.a2a.model.Part
import org.http4k.ai.a2a.model.Task
import org.http4k.ai.a2a.model.TaskId
import org.http4k.ai.a2a.model.TaskState
import org.http4k.ai.a2a.model.TaskStatus
import org.http4k.ai.a2a.protocol.messages.A2AMessage
import org.http4k.ai.a2a.model.PushNotificationConfig
import org.http4k.ai.a2a.server.capability.pushNotificationConfigs
import org.http4k.ai.a2a.server.capability.tasks
import org.http4k.ai.a2a.server.http.a2a
import org.http4k.ai.a2a.server.protocol.A2AProtocol
import org.http4k.ai.a2a.MessageHandler
import org.http4k.ai.a2a.MessageResponse
import org.http4k.ai.model.Role
import org.http4k.core.HttpHandler
import org.http4k.core.Uri
import org.junit.jupiter.api.Test
import java.util.UUID

abstract class A2AClientContract {

    protected val agentCard = AgentCard(
        name = "Test Agent",
        url = Uri.of("http://localhost:8080"),
        version = "1.0.0",
        capabilities = AgentCapabilities(streaming = false)
    )

    protected val tasks = tasks()
    protected val pushNotificationConfigs = pushNotificationConfigs()

    protected val messageHandler: MessageHandler = { request ->
        val taskId = TaskId.of(UUID.randomUUID().toString())
        val contextId = ContextId.of(UUID.randomUUID().toString())

        val task = Task(
            id = taskId,
            contextId = contextId,
            status = TaskStatus(state = TaskState.completed),
            history = listOf(request.message)
        )
        tasks.store(task)
        MessageResponse.Task(sequenceOf(task))
    }

    protected val protocol = A2AProtocol(agentCard, messageHandler, tasks, pushNotificationConfigs)

    abstract fun clientFor(server: HttpHandler): A2AClient

    private fun withServer(test: A2AClient.() -> Unit) {
        val server = a2a(protocol)
        val client = clientFor(server)
        try {
            client.test()
        } finally {
            client.close()
        }
    }

    @Test
    fun `can get agent card`() = withServer {
        val result = agentCard()
        val card = result.valueOrNull()!!
        assertThat(card.name, equalTo("Test Agent"))
        assertThat(card.version, equalTo("1.0.0"))
    }

    @Test
    fun `can send message and receive task`() = withServer {
        val message = Message(
            role = Role.User,
            parts = listOf(Part.Text("Hello, agent!"))
        )

        val result = message(message)
        val response = result.valueOrNull()!!

        assertThat(response is A2AMessage.Send.Response.Task, equalTo(true))
        val taskResponse = response as A2AMessage.Send.Response.Task
        assertThat(taskResponse.task.status.state, equalTo(TaskState.completed))
    }

    @Test
    fun `can get task by id`() = withServer {
        val message = Message(
            role = Role.User,
            parts = listOf(Part.Text("Hello"))
        )
        val sendResult = message(message)
        val taskResponse = sendResult.valueOrNull()!! as A2AMessage.Send.Response.Task
        val taskId = taskResponse.task.id

        val result = tasks().get(taskId)
        val task = result.valueOrNull()!!
        assertThat(task.id, equalTo(taskId))
    }

    @Test
    fun `can cancel task`() = withServer {
        val message = Message(
            role = Role.User,
            parts = listOf(Part.Text("Hello"))
        )
        val sendResult = message(message)
        val taskResponse = sendResult.valueOrNull()!! as A2AMessage.Send.Response.Task
        val taskId = taskResponse.task.id

        val result = tasks().cancel(taskId)
        val task = result.valueOrNull()!!
        assertThat(task.id, equalTo(taskId))
        assertThat(task.status.state, equalTo(TaskState.canceled))
    }

    @Test
    fun `can send message and receive message response`() {
        val messageResponseHandler: MessageHandler = { request ->
            MessageResponse.Message(
                Message(
                    role = Role.Assistant,
                    parts = listOf(Part.Text("Response to: ${request.message.parts}"))
                )
            )
        }

        val messageProtocol = A2AProtocol(agentCard, messageResponseHandler, tasks)
        val server = a2a(messageProtocol)
        val client = clientFor(server)

        try {
            val message = Message(
                role = Role.User,
                parts = listOf(Part.Text("Hello"))
            )
            val result = client.message(message)
            val response = result.valueOrNull()!!

            assertThat(response is A2AMessage.Send.Response.Message, equalTo(true))
        } finally {
            client.close()
        }
    }

    @Test
    fun `can stream message and receive task responses`() {
        val streamingHandler: MessageHandler = { request ->
            val taskId = TaskId.of(UUID.randomUUID().toString())
            val contextId = ContextId.of(UUID.randomUUID().toString())

            MessageResponse.Task(
                sequenceOf(
                    Task(
                        id = taskId,
                        contextId = contextId,
                        status = TaskStatus(state = TaskState.working),
                        history = listOf(request.message)
                    ),
                    Task(
                        id = taskId,
                        contextId = contextId,
                        status = TaskStatus(state = TaskState.completed),
                        history = listOf(request.message)
                    )
                )
            )
        }

        val streamingAgentCard = agentCard.copy(capabilities = AgentCapabilities(streaming = true))
        val streamingProtocol = A2AProtocol(streamingAgentCard, streamingHandler, tasks)
        val server = a2a(streamingProtocol)
        val client = clientFor(server)

        try {
            val message = Message(
                role = Role.User,
                parts = listOf(Part.Text("Hello, streaming agent!"))
            )

            val result = client.messageStream(message)
            val responses = result.valueOrNull()!!.toList()

            assertThat(responses.size, equalTo(2))
            assertThat(responses[0] is A2AMessage.Send.Response.Task, equalTo(true))
            assertThat(
                (responses[0] as A2AMessage.Send.Response.Task).task.status.state,
                equalTo(TaskState.working)
            )
            assertThat(
                (responses[1] as A2AMessage.Send.Response.Task).task.status.state,
                equalTo(TaskState.completed)
            )
        } finally {
            client.close()
        }
    }

    @Test
    fun `can list tasks`() = withServer {
        val message = Message(
            role = Role.User,
            parts = listOf(Part.Text("Hello"))
        )
        message(message)
        message(message)

        val result = tasks().list()
        val response = result.valueOrNull()!!
        assertThat(response.tasks.size, equalTo(2))
        assertThat(response.totalSize, equalTo(2))
    }

    @Test
    fun `can set push notification config`() = withServer {
        val message = Message(
            role = Role.User,
            parts = listOf(Part.Text("Hello"))
        )
        val sendResult = message(message)
        val taskResponse = sendResult.valueOrNull()!! as A2AMessage.Send.Response.Task
        val taskId = taskResponse.task.id

        val config = PushNotificationConfig(url = Uri.of("https://example.com/webhook"))
        val result = pushNotificationConfigs().set(taskId, config)
        val taskPushConfig = result.valueOrNull()!!

        assertThat(taskPushConfig.taskId, equalTo(taskId))
        assertThat(taskPushConfig.pushNotificationConfig.url, equalTo(Uri.of("https://example.com/webhook")))
    }

    @Test
    fun `can get push notification config`() = withServer {
        val message = Message(
            role = Role.User,
            parts = listOf(Part.Text("Hello"))
        )
        val sendResult = message(message)
        val taskResponse = sendResult.valueOrNull()!! as A2AMessage.Send.Response.Task
        val taskId = taskResponse.task.id

        val config = PushNotificationConfig(url = Uri.of("https://example.com/webhook"))
        val setResult = pushNotificationConfigs().set(taskId, config)
        val setResponse = setResult.valueOrNull()!!

        val result = pushNotificationConfigs().get(setResponse.id)
        val taskPushConfig = result.valueOrNull()!!

        assertThat(taskPushConfig.id, equalTo(setResponse.id))
        assertThat(taskPushConfig.taskId, equalTo(taskId))
    }

    @Test
    fun `can list push notification configs`() = withServer {
        val message = Message(
            role = Role.User,
            parts = listOf(Part.Text("Hello"))
        )
        val sendResult = message(message)
        val taskResponse = sendResult.valueOrNull()!! as A2AMessage.Send.Response.Task
        val taskId = taskResponse.task.id

        pushNotificationConfigs().set(taskId, PushNotificationConfig(url = Uri.of("https://example.com/webhook1")))
        pushNotificationConfigs().set(taskId, PushNotificationConfig(url = Uri.of("https://example.com/webhook2")))

        val result = pushNotificationConfigs().list(taskId)
        val configs = result.valueOrNull()!!

        assertThat(configs.size, equalTo(2))
    }

    @Test
    fun `can delete push notification config`() = withServer {
        val message = Message(
            role = Role.User,
            parts = listOf(Part.Text("Hello"))
        )
        val sendResult = message(message)
        val taskResponse = sendResult.valueOrNull()!! as A2AMessage.Send.Response.Task
        val taskId = taskResponse.task.id

        val config = PushNotificationConfig(url = Uri.of("https://example.com/webhook"))
        val setResult = pushNotificationConfigs().set(taskId, config)
        val setResponse = setResult.valueOrNull()!!

        val deleteResult = pushNotificationConfigs().delete(setResponse.id)
        val deletedId = deleteResult.valueOrNull()!!

        assertThat(deletedId, equalTo(setResponse.id))

        val listResult = pushNotificationConfigs().list(taskId)
        assertThat(listResult.valueOrNull()!!.size, equalTo(0))
    }
}
