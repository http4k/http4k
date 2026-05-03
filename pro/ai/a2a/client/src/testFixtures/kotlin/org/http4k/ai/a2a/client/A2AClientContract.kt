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
import org.http4k.ai.a2a.model.Part
import org.http4k.ai.a2a.model.ResponseStream
import org.http4k.ai.a2a.model.SkillId
import org.http4k.ai.a2a.model.Task
import org.http4k.ai.a2a.model.TaskId
import org.http4k.ai.a2a.model.TaskState.*
import org.http4k.ai.a2a.model.TaskStatus
import org.http4k.ai.a2a.model.Version
import org.http4k.ai.a2a.protocol.messages.SendMessageConfiguration
import org.http4k.ai.a2a.server.storage.PushNotificationConfigStorage
import org.http4k.ai.a2a.server.storage.TaskStorage
import org.http4k.ai.a2a.model.A2ARole
import org.http4k.connect.model.MimeType
import org.http4k.core.PolyHandler
import org.http4k.core.Uri
import org.http4k.server.Helidon
import org.http4k.server.asServer
import org.junit.jupiter.api.Test

abstract class A2AClientContract {

    private var idCounter = 0
    private var msgCounter = 0

    private fun nextMessageId() = MessageId.of("msg-${++msgCounter}")

    protected val agentCard = AgentCard(
        name = "Test Agent",
        url = Uri.of("http://localhost:8080"),
        version = Version.of("1.0.0"),
        description = "Test agent for contract tests",
        capabilities = AgentCapabilities(streaming = false, pushNotifications = true, extendedAgentCard = true)
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
    ): PolyHandler

    abstract fun clientFor(port: Int): A2AClient

    private fun withServer(
        cards: AgentCardProvider = AgentCardProvider(agentCard),
        handler: MessageHandler = messageHandler,
        test: A2AClient.() -> Unit
    ) {
        val server = serverFor(cards, handler, tasks, pushNotificationConfigs).asServer(Helidon(0)).start()
        val client = clientFor(server.port())
        try {
            client.test()
        } finally {
            client.close()
            server.stop()
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
        val response = message(Message(nextMessageId(), A2ARole.ROLE_USER, listOf(Part.Text("Hello")))).valueOrNull()!!
        assertThat(response, isA<Task>())
        assertThat((response as Task).status.state, equalTo(TASK_STATE_COMPLETED))
    }

    @Test
    fun `can get task by id`() = withServer {
        val taskResponse = message(Message(nextMessageId(), A2ARole.ROLE_USER, listOf(Part.Text("Hello")))).valueOrNull()!! as Task
        val task = tasks().get(taskResponse.id).valueOrNull()!!
        assertThat(task.id, equalTo(taskResponse.id))
    }

    @Test
    fun `can cancel task`() = withServer {
        val taskResponse = message(Message(nextMessageId(), A2ARole.ROLE_USER, listOf(Part.Text("Hello")))).valueOrNull()!! as Task
        val task = tasks().cancel(taskResponse.id).valueOrNull()!!
        assertThat(task.status.state, equalTo(TASK_STATE_CANCELED))
    }

    @Test
    fun `can send message and receive message response`() = withServer(
        handler = { Message(nextMessageId(), A2ARole.ROLE_AGENT, listOf(Part.Text("response"))) }
    ) {
        val response = message(Message(nextMessageId(), A2ARole.ROLE_USER, listOf(Part.Text("Hello")))).valueOrNull()!!
        assertThat(response, isA<Message>())
    }

    @Test
    fun `can stream message and receive task responses`() {
        var streamCounter = 0
        val streamingHandler: MessageHandler = { request ->
            val count = ++streamCounter
            ResponseStream(
                sequenceOf(
                    Task(TaskId.of("st-$count"), ContextId.of("sc-$count"), TaskStatus(state = TASK_STATE_WORKING), history = listOf(request.message)),
                    Task(TaskId.of("st-$count"), ContextId.of("sc-$count"), TaskStatus(state = TASK_STATE_COMPLETED), history = listOf(request.message))
                )
            )
        }

        withServer(
            cards = AgentCardProvider(agentCard.copy(capabilities = agentCard.capabilities.copy(streaming = true))),
            handler = streamingHandler
        ) {
            val response = messageStream(Message(nextMessageId(), A2ARole.ROLE_USER, listOf(Part.Text("Hello")))).valueOrNull()!!
            val streamTasks = (response as ResponseStream).toList().filterIsInstance<Task>()
            assertThat(streamTasks.size, equalTo(2))
            assertThat(streamTasks[0].status.state, equalTo(TASK_STATE_WORKING))
            assertThat(streamTasks[1].status.state, equalTo(TASK_STATE_COMPLETED))
        }
    }

    @Test
    fun `can subscribe to task and receive current state`() {
        withServer(
            cards = AgentCardProvider(agentCard.copy(capabilities = agentCard.capabilities.copy(streaming = true)))
        ) {
            val taskResponse = message(Message(nextMessageId(), A2ARole.ROLE_USER, listOf(Part.Text("Hello")))).valueOrNull()!! as Task
            val response = tasks().subscribe(taskResponse.id).valueOrNull()!!
            val items = (response as ResponseStream).take(1).toList()
            assertThat(items.size, equalTo(1))
            assertThat((items[0] as Task).id, equalTo(taskResponse.id))
        }
    }

    @Test
    fun `can list tasks`() = withServer {
        message(Message(nextMessageId(), A2ARole.ROLE_USER, listOf(Part.Text("Hello"))))
        message(Message(nextMessageId(), A2ARole.ROLE_USER, listOf(Part.Text("Hello"))))

        val page = tasks().list().valueOrNull()!!
        assertThat(page.tasks.size, equalTo(2))
        assertThat(page.totalSize, equalTo(2))
    }

    @Test
    fun `can set push notification config`() = withServer {
        val taskResponse = message(Message(nextMessageId(), A2ARole.ROLE_USER, listOf(Part.Text("Hello")))).valueOrNull()!! as Task
        val config = pushNotificationConfigs().set(taskResponse.id, Uri.of("https://example.com/webhook")).valueOrNull()!!
        assertThat(config.taskId, equalTo(taskResponse.id))
    }

    @Test
    fun `can get push notification config`() = withServer {
        val taskResponse = message(Message(nextMessageId(), A2ARole.ROLE_USER, listOf(Part.Text("Hello")))).valueOrNull()!! as Task
        val taskId = taskResponse.id
        val setResponse = pushNotificationConfigs().set(taskId, Uri.of("https://example.com/webhook")).valueOrNull()!!
        val config = pushNotificationConfigs().get(taskId, setResponse.id).valueOrNull()!!
        assertThat(config.id, equalTo(setResponse.id))
    }

    @Test
    fun `can list push notification configs`() = withServer {
        val taskResponse = message(Message(nextMessageId(), A2ARole.ROLE_USER, listOf(Part.Text("Hello")))).valueOrNull()!! as Task
        val taskId = taskResponse.id
        pushNotificationConfigs().set(taskId, Uri.of("https://a.com"))
        pushNotificationConfigs().set(taskId, Uri.of("https://b.com"))
        assertThat(pushNotificationConfigs().list(taskId).valueOrNull()!!.size, equalTo(2))
    }

    @Test
    fun `can delete push notification config`() = withServer {
        val taskResponse = message(Message(nextMessageId(), A2ARole.ROLE_USER, listOf(Part.Text("Hello")))).valueOrNull()!! as Task
        val taskId = taskResponse.id
        val setResponse = pushNotificationConfigs().set(taskId, Uri.of("https://example.com/webhook")).valueOrNull()!!
        pushNotificationConfigs().delete(taskId, setResponse.id).valueOrNull()!!
        assertThat(pushNotificationConfigs().list(taskId).valueOrNull()!!.size, equalTo(0))
    }

    @Test
    fun `send passes configuration and metadata through`() {
        val config = SendMessageConfiguration(acceptedOutputModes = listOf(MimeType.of("text/plain")))
        val metadata = mapOf("key" to "value")
        var receivedConfig: SendMessageConfiguration? = null
        var receivedMetadata: Map<String, Any>? = null

        withServer(handler = { request ->
            receivedConfig = request.configuration
            receivedMetadata = request.metadata
            Message(nextMessageId(), A2ARole.ROLE_AGENT, listOf(Part.Text("ok")))
        }) {
            message(Message(nextMessageId(), A2ARole.ROLE_USER, listOf(Part.Text("Hello"))), configuration = config, metadata = metadata).valueOrNull()!!
            assertThat(receivedConfig, equalTo(config))
            assertThat(receivedMetadata, equalTo(metadata))
        }
    }

    @Test
    fun `get task with historyLength trims history`() {
        val multiHistoryHandler: MessageHandler = { request ->
            val count = ++idCounter
            val messages = (1..5).map { Message(MessageId.of("h-$it"), A2ARole.ROLE_USER, listOf(Part.Text("msg $it"))) }
            val task = Task(TaskId.of("task-$count"), ContextId.of("ctx-$count"), TaskStatus(state = TASK_STATE_COMPLETED), history = messages)
            tasks.store(task)
            task
        }

        withServer(handler = multiHistoryHandler) {
            val taskResponse = message(Message(nextMessageId(), A2ARole.ROLE_USER, listOf(Part.Text("Hello")))).valueOrNull()!! as Task
            val task = tasks().get(taskResponse.id, historyLength = 2).valueOrNull()!!
            assertThat(task.history!!.size, equalTo(2))
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

        withServer(handler = artifactHandler) {
            message(Message(nextMessageId(), A2ARole.ROLE_USER, listOf(Part.Text("Hello")))).valueOrNull()!!
            val page = tasks().list(includeArtifacts = false).valueOrNull()!!
            assertThat(page.tasks.first().artifacts, absent())
        }
    }

    @Test
    fun `can get extended agent card`() {
        val extendedCard = agentCard.copy(
            capabilities = AgentCapabilities(streaming = true, extendedAgentCard = true, pushNotifications = true),
            skills = listOf(AgentSkill(id = SkillId.of("secret"), name = "Secret Skill", description = "Only for authenticated users"))
        )

        withServer(cards = AgentCardProvider(agentCard, extendedCard)) {
            val card = extendedAgentCard().valueOrNull()!!
            assertThat(card.skills.first().name, equalTo("Secret Skill"))
        }
    }
}
