/*
 * Copyright (c) 2025-present http4k Ltd. All rights reserved.
 * Licensed under the http4k Commercial License: https://http4k.org/commercial-license
 */
package org.http4k.ai.a2a.client

import com.natpryce.hamkrest.absent
import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.equalTo
import com.natpryce.hamkrest.isA
import dev.forkhandles.result4k.valueOrNull
import org.http4k.ai.a2a.MessageHandler
import org.http4k.ai.a2a.model.AgentCapabilities
import org.http4k.ai.a2a.model.AgentCard
import org.http4k.ai.a2a.model.AgentCardProvider
import org.http4k.ai.a2a.model.AgentSkill
import org.http4k.ai.a2a.model.Artifact
import org.http4k.ai.a2a.model.ArtifactId
import org.http4k.ai.a2a.model.ContextId
import org.http4k.ai.a2a.model.Message
import org.http4k.ai.a2a.model.MessageId
import org.http4k.ai.a2a.model.MessageStream
import org.http4k.ai.a2a.model.Part
import org.http4k.ai.a2a.model.PushNotificationConfig
import org.http4k.ai.a2a.model.SkillId
import org.http4k.ai.a2a.model.Task
import org.http4k.ai.a2a.model.TaskId
import org.http4k.ai.a2a.model.TaskState.*
import org.http4k.ai.a2a.model.TaskStatus
import org.http4k.ai.a2a.model.Version
import org.http4k.ai.a2a.protocol.messages.TaskConfiguration
import org.http4k.ai.a2a.server.storage.PushNotificationConfigStorage
import org.http4k.ai.a2a.server.storage.TaskStorage
import org.http4k.ai.model.Role
import org.http4k.connect.model.MimeType
import org.http4k.core.HttpHandler
import org.http4k.core.Uri
import org.junit.jupiter.api.Test

abstract class A2AClientContract {

    private var idCounter = 0
    private var msgCounter = 0

    private fun nextMessageId() = MessageId.of("msg-${++msgCounter}")

    protected val agentCard = AgentCard(
        name = "Test Agent",
        url = Uri.of("http://localhost:8080"),
        version = Version.of("1.0.0"),
        capabilities = AgentCapabilities(streaming = false)
    )

    protected val tasks = TaskStorage.InMemory()
    protected val pushNotificationConfigs = PushNotificationConfigStorage.InMemory()

    protected val messageHandler: MessageHandler = { request ->
        val count = ++idCounter
        val taskId = TaskId.of("task-$count")
        val contextId = ContextId.of("context-$count")

        val task = Task(
            id = taskId,
            contextId = contextId,
            status = TaskStatus(state = TASK_STATE_COMPLETED),
            history = listOf(request.message)
        )
        tasks.store(task)
        task
    }

    abstract fun serverFor(
        cards: AgentCardProvider,
        handler: MessageHandler,
        tasks: TaskStorage,
        pushNotifications: PushNotificationConfigStorage
    ): HttpHandler

    abstract fun clientFor(server: HttpHandler): A2AClient

    private fun withServer(test: A2AClient.() -> Unit) {
        val server = serverFor(AgentCardProvider(agentCard), messageHandler, tasks, pushNotificationConfigs)
        val client = clientFor(server)
        try {
            client.test()
        } finally {
            client.close()
        }
    }

    @Test
    fun `can get agent card`() = withServer {
        val card = agentCard().valueOrNull()!!
        assertThat(card.name, equalTo("Test Agent"))
        assertThat(card.version, equalTo(Version.of("1.0.0")))
    }

    @Test
    fun `can send message and receive task`() = withServer {
        val response = message(Message(nextMessageId(), Role.User, listOf(Part.Text("Hello")))).valueOrNull()!!
        assertThat(response, isA<Task>())
        assertThat((response as Task).status.state, equalTo(TASK_STATE_COMPLETED))
    }

    @Test
    fun `can get task by id`() = withServer {
        val taskResponse = message(Message(nextMessageId(), Role.User, listOf(Part.Text("Hello")))).valueOrNull()!! as Task
        val task = tasks().get(taskResponse.id).valueOrNull()!!
        assertThat(task.id, equalTo(taskResponse.id))
    }

    @Test
    fun `can cancel task`() = withServer {
        val taskResponse = message(Message(nextMessageId(), Role.User, listOf(Part.Text("Hello")))).valueOrNull()!! as Task
        val task = tasks().cancel(taskResponse.id).valueOrNull()!!
        assertThat(task.status.state, equalTo(TASK_STATE_CANCELED))
    }

    @Test
    fun `can send message and receive message response`() {
        val messageResponseHandler: MessageHandler = {
            Message(nextMessageId(), Role.Assistant, listOf(Part.Text("response")))
        }

        val server = serverFor(AgentCardProvider(agentCard), messageResponseHandler, tasks, pushNotificationConfigs)
        val client = clientFor(server)

        try {
            val response = client.message(Message(nextMessageId(), Role.User, listOf(Part.Text("Hello")))).valueOrNull()!!
            assertThat(response, isA<Message>())
        } finally {
            client.close()
        }
    }

    @Test
    fun `can stream message and receive task responses`() {
        var streamCounter = 0
        val streamingHandler: MessageHandler = { request ->
            val count = ++streamCounter
            MessageStream(
                sequenceOf(
                    Task(
                        TaskId.of("st-$count"),
                        ContextId.of("sc-$count"),
                        TaskStatus(state = TASK_STATE_WORKING),
                        history = listOf(request.message)
                    ),
                    Task(
                        TaskId.of("st-$count"),
                        ContextId.of("sc-$count"),
                        TaskStatus(state = TASK_STATE_COMPLETED),
                        history = listOf(request.message)
                    )
                )
            )
        }

        val server = serverFor(AgentCardProvider(agentCard.copy(capabilities = AgentCapabilities(streaming = true))), streamingHandler, tasks, pushNotificationConfigs)
        val client = clientFor(server)

        try {
            val response = client.messageStream(Message(nextMessageId(), Role.User, listOf(Part.Text("Hello")))).valueOrNull()!!
            val streamTasks = (response as MessageStream).toList().filterIsInstance<Task>()
            assertThat(streamTasks.size, equalTo(2))
            assertThat(streamTasks[0].status.state, equalTo(TASK_STATE_WORKING))
            assertThat(streamTasks[1].status.state, equalTo(TASK_STATE_COMPLETED))
        } finally {
            client.close()
        }
    }

    @Test
    fun `can list tasks`() = withServer {
        message(Message(nextMessageId(), Role.User, listOf(Part.Text("Hello"))))
        message(Message(nextMessageId(), Role.User, listOf(Part.Text("Hello"))))

        val page = tasks().list().valueOrNull()!!
        assertThat(page.tasks.size, equalTo(2))
        assertThat(page.totalSize, equalTo(2))
    }

    @Test
    fun `can set push notification config`() = withServer {
        val taskResponse = message(Message(nextMessageId(), Role.User, listOf(Part.Text("Hello")))).valueOrNull()!! as Task
        val config = pushNotificationConfigs().set(taskResponse.id, PushNotificationConfig(url = Uri.of("https://example.com/webhook"))).valueOrNull()!!
        assertThat(config.taskId, equalTo(taskResponse.id))
    }

    @Test
    fun `can get push notification config`() = withServer {
        val taskResponse = message(Message(nextMessageId(), Role.User, listOf(Part.Text("Hello")))).valueOrNull()!! as Task
        val taskId = taskResponse.id
        val setResponse = pushNotificationConfigs().set(taskId, PushNotificationConfig(url = Uri.of("https://example.com/webhook"))).valueOrNull()!!
        val config = pushNotificationConfigs().get(taskId, setResponse.id).valueOrNull()!!
        assertThat(config.id, equalTo(setResponse.id))
    }

    @Test
    fun `can list push notification configs`() = withServer {
        val taskResponse = message(Message(nextMessageId(), Role.User, listOf(Part.Text("Hello")))).valueOrNull()!! as Task
        val taskId = taskResponse.id
        pushNotificationConfigs().set(taskId, PushNotificationConfig(url = Uri.of("https://a.com")))
        pushNotificationConfigs().set(taskId, PushNotificationConfig(url = Uri.of("https://b.com")))
        assertThat(pushNotificationConfigs().list(taskId).valueOrNull()!!.size, equalTo(2))
    }

    @Test
    fun `can delete push notification config`() = withServer {
        val taskResponse = message(Message(nextMessageId(), Role.User, listOf(Part.Text("Hello")))).valueOrNull()!! as Task
        val taskId = taskResponse.id
        val setResponse = pushNotificationConfigs().set(taskId, PushNotificationConfig(url = Uri.of("https://example.com/webhook"))).valueOrNull()!!
        pushNotificationConfigs().delete(taskId, setResponse.id).valueOrNull()!!
        assertThat(pushNotificationConfigs().list(taskId).valueOrNull()!!.size, equalTo(0))
    }

    @Test
    fun `send passes configuration and metadata through`() {
        val config = TaskConfiguration(acceptedOutputModes = listOf(MimeType.of("text/plain")))
        val metadata = mapOf("key" to "value")
        var receivedConfig: TaskConfiguration? = null
        var receivedMetadata: Map<String, Any>? = null

        val handler: MessageHandler = { request ->
            receivedConfig = request.configuration
            receivedMetadata = request.metadata
            Message(nextMessageId(), Role.Assistant, listOf(Part.Text("ok")))
        }

        val server = serverFor(AgentCardProvider(agentCard), handler, tasks, pushNotificationConfigs)
        val client = clientFor(server)

        try {
            client.message(Message(nextMessageId(), Role.User, listOf(Part.Text("Hello"))), configuration = config, metadata = metadata).valueOrNull()!!
            assertThat(receivedConfig, equalTo(config))
            assertThat(receivedMetadata, equalTo(metadata))
        } finally {
            client.close()
        }
    }

    @Test
    fun `get task with historyLength trims history`() {
        val multiHistoryHandler: MessageHandler = { request ->
            val count = ++idCounter
            val messages = (1..5).map { Message(MessageId.of("h-$it"), Role.User, listOf(Part.Text("msg $it"))) }
            val task = Task(TaskId.of("task-$count"), ContextId.of("ctx-$count"), TaskStatus(state = TASK_STATE_COMPLETED), history = messages)
            tasks.store(task)
            task
        }

        val server = serverFor(AgentCardProvider(agentCard), multiHistoryHandler, tasks, pushNotificationConfigs)
        val client = clientFor(server)

        try {
            val taskResponse = client.message(Message(nextMessageId(), Role.User, listOf(Part.Text("Hello")))).valueOrNull()!! as Task
            val task = client.tasks().get(taskResponse.id, historyLength = 2).valueOrNull()!!
            assertThat(task.history!!.size, equalTo(2))
        } finally {
            client.close()
        }
    }

    @Test
    fun `list tasks with includeArtifacts false strips artifacts`() {
        val artifactHandler: MessageHandler = { request ->
            val count = ++idCounter
            val task = Task(
                TaskId.of("task-$count"), ContextId.of("ctx-$count"),
                TaskStatus(state = TASK_STATE_COMPLETED),
                artifacts = listOf(Artifact(ArtifactId.of("a1"), listOf(Part.Text("artifact"))))
            )
            tasks.store(task)
            task
        }

        val server = serverFor(AgentCardProvider(agentCard), artifactHandler, tasks, pushNotificationConfigs)
        val client = clientFor(server)

        try {
            client.message(Message(nextMessageId(), Role.User, listOf(Part.Text("Hello")))).valueOrNull()!!
            val page = client.tasks().list(includeArtifacts = false).valueOrNull()!!
            assertThat(page.tasks.first().artifacts, absent())
        } finally {
            client.close()
        }
    }

    @Test
    fun `can get extended agent card`() {
        val extendedCard = agentCard.copy(
            capabilities = AgentCapabilities(streaming = true, extendedAgentCard = true),
            skills = listOf(AgentSkill(id = SkillId.of("secret"), name = "Secret Skill", description = "Only for authenticated users"))
        )

        val server = serverFor(AgentCardProvider(agentCard, extendedCard), messageHandler, tasks, pushNotificationConfigs)
        val client = clientFor(server)

        try {
            val card = client.extendedAgentCard().valueOrNull()!!
            assertThat(card.skills!!.first().name, equalTo("Secret Skill"))
        } finally {
            client.close()
        }
    }
}
