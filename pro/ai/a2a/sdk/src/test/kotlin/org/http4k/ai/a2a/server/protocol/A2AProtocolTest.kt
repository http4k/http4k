/*
 * Copyright (c) 2025-present http4k Ltd. All rights reserved.
 * Licensed under the http4k Commercial License: https://http4k.org/commercial-license
 */
package org.http4k.ai.a2a.server.protocol

import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.equalTo
import dev.forkhandles.result4k.valueOrNull
import org.http4k.ai.a2a.model.AgentCapabilities
import org.http4k.ai.a2a.model.AgentCard
import org.http4k.ai.a2a.model.ContextId
import org.http4k.ai.a2a.model.Message
import org.http4k.ai.a2a.model.Part
import org.http4k.ai.a2a.model.PushNotificationConfigId
import org.http4k.ai.a2a.model.Task
import org.http4k.ai.a2a.model.TaskId
import org.http4k.ai.a2a.model.TaskState
import org.http4k.ai.a2a.model.TaskStatus
import org.http4k.ai.a2a.protocol.messages.A2AMessage
import org.http4k.ai.a2a.protocol.messages.A2APushNotificationConfig
import org.http4k.ai.a2a.protocol.messages.A2ATask
import org.http4k.ai.a2a.model.PushNotificationConfig
import org.http4k.ai.a2a.MessageHandler
import org.http4k.ai.a2a.MessageResponse
import org.http4k.ai.a2a.server.capability.pushNotificationConfigs
import org.http4k.ai.a2a.server.capability.tasks
import org.http4k.ai.a2a.util.A2AJson
import org.http4k.ai.model.Role
import org.http4k.core.Method.POST
import org.http4k.core.Request
import org.http4k.core.Uri
import org.junit.jupiter.api.Test
import java.util.UUID

class A2AProtocolTest {

    private val agentCard = AgentCard(
        name = "Test Agent",
        url = Uri.of("http://localhost:8080"),
        version = "1.0.0",
        capabilities = AgentCapabilities(streaming = true)
    )

    @Test
    fun `message send returns Single response when handler yields one item`() {
        val handler: MessageHandler = { request ->
            MessageResponse.Task(
                sequenceOf(
                    Task(
                        id = TaskId.of(UUID.randomUUID().toString()),
                        contextId = ContextId.of(UUID.randomUUID().toString()),
                        status = TaskStatus(state = TaskState.completed),
                        history = listOf(request.message)
                    )
                )
            )
        }

        val protocol = A2AProtocol(agentCard, handler)

        val message = Message(role = Role.User, parts = listOf(Part.Text("Hello")))
        val jsonRequest = A2AJson.asFormatString(
            A2AMessage.Send.Request(A2AMessage.Send.Request.Params(message), "1")
        )

        val result = protocol(Request(POST, "/").body(jsonRequest))
        val response = result.valueOrNull()!!

        assertThat(response is A2AProtocolResponse.Single, equalTo(true))
    }

    @Test
    fun `message stream returns Stream response with sequence`() {
        val handler: MessageHandler = { request ->
            MessageResponse.Task(
                sequenceOf(
                    Task(
                        id = TaskId.of(UUID.randomUUID().toString()),
                        contextId = ContextId.of(UUID.randomUUID().toString()),
                        status = TaskStatus(state = TaskState.working),
                        history = listOf(request.message)
                    ),
                    Task(
                        id = TaskId.of(UUID.randomUUID().toString()),
                        contextId = ContextId.of(UUID.randomUUID().toString()),
                        status = TaskStatus(state = TaskState.completed),
                        history = listOf(request.message)
                    )
                )
            )
        }

        val protocol = A2AProtocol(agentCard, handler)

        val message = Message(role = Role.User, parts = listOf(Part.Text("Hello")))
        val jsonRequest = A2AJson.asFormatString(
            A2AMessage.Stream.Request(A2AMessage.Stream.Request.Params(message), "1")
        )

        val result = protocol(Request(POST, "/").body(jsonRequest))
        val response = result.valueOrNull()!!

        assertThat(response is A2AProtocolResponse.Stream, equalTo(true))
        val streamResponse = response as A2AProtocolResponse.Stream
        assertThat(streamResponse.responses.count(), equalTo(2))
    }

    @Test
    fun `tasks list returns list of tasks`() {
        val tasks = tasks()
        tasks.store(
            Task(
                id = TaskId.of("task-1"),
                contextId = ContextId.of("ctx-1"),
                status = TaskStatus(state = TaskState.working)
            )
        )
        tasks.store(
            Task(
                id = TaskId.of("task-2"),
                contextId = ContextId.of("ctx-1"),
                status = TaskStatus(state = TaskState.completed)
            )
        )

        val protocol = A2AProtocol(agentCard, { MessageResponse.Task(emptySequence()) }, tasks)

        val jsonRequest = A2AJson.asFormatString(
            A2ATask.List.Request(A2ATask.List.Request.Params(), "1")
        )

        val result = protocol(Request(POST, "/").body(jsonRequest))
        val response = result.valueOrNull()!!

        assertThat(response is A2AProtocolResponse.Single, equalTo(true))
        val singleResponse = response as A2AProtocolResponse.Single
        val json = A2AJson.asFormatString(singleResponse.response)
        assertThat(json.contains("\"totalSize\":2"), equalTo(true))
    }

    @Test
    fun `pushNotificationConfig set creates config`() {
        val configs = pushNotificationConfigs()
        val protocol =
            A2AProtocol(agentCard, { MessageResponse.Task(emptySequence()) }, pushNotificationConfigs = configs)

        val jsonRequest = A2AJson.asFormatString(
            A2APushNotificationConfig.Set.Request(
                A2APushNotificationConfig.Set.Request.Params(
                    taskId = TaskId.of("task-1"),
                    pushNotificationConfig = PushNotificationConfig(url = Uri.of("https://example.com/webhook"))
                ),
                "1"
            )
        )

        val result = protocol(Request(POST, "/").body(jsonRequest))
        val response = result.valueOrNull()!!

        assertThat(response is A2AProtocolResponse.Single, equalTo(true))
        val singleResponse = response as A2AProtocolResponse.Single
        val json = A2AJson.asFormatString(singleResponse.response)
        assertThat(json.contains("\"taskId\":\"task-1\""), equalTo(true))
        assertThat(json.contains("\"url\":\"https://example.com/webhook\""), equalTo(true))
    }

    @Test
    fun `pushNotificationConfig get returns config`() {
        val configs = pushNotificationConfigs()
        val created = configs.set(
            A2APushNotificationConfig.Set.Request.Params(
                taskId = TaskId.of("task-1"),
                pushNotificationConfig = PushNotificationConfig(url = Uri.of("https://example.com/webhook"))
            )
        )
        val protocol =
            A2AProtocol(agentCard, { MessageResponse.Task(emptySequence()) }, pushNotificationConfigs = configs)

        val jsonRequest = A2AJson.asFormatString(
            A2APushNotificationConfig.Get.Request(
                A2APushNotificationConfig.Get.Request.Params(id = created.id),
                "1"
            )
        )

        val result = protocol(Request(POST, "/").body(jsonRequest))
        val response = result.valueOrNull()!!

        assertThat(response is A2AProtocolResponse.Single, equalTo(true))
        val singleResponse = response as A2AProtocolResponse.Single
        val json = A2AJson.asFormatString(singleResponse.response)
        assertThat(json.contains("\"taskId\":\"task-1\""), equalTo(true))
    }

    @Test
    fun `pushNotificationConfig get returns error for non-existent config`() {
        val configs = pushNotificationConfigs()
        val protocol =
            A2AProtocol(agentCard, { MessageResponse.Task(emptySequence()) }, pushNotificationConfigs = configs)

        val jsonRequest = A2AJson.asFormatString(
            A2APushNotificationConfig.Get.Request(
                A2APushNotificationConfig.Get.Request.Params(
                    id = PushNotificationConfigId.of("non-existent")
                ),
                "1"
            )
        )

        val result = protocol(Request(POST, "/").body(jsonRequest))
        val response = result.valueOrNull()!!

        assertThat(response is A2AProtocolResponse.Single, equalTo(true))
        val singleResponse = response as A2AProtocolResponse.Single
        val json = A2AJson.asFormatString(singleResponse.response)
        assertThat(json.contains("\"error\""), equalTo(true))
    }

    @Test
    fun `pushNotificationConfig list returns configs for task`() {
        val configs = pushNotificationConfigs()
        configs.set(
            A2APushNotificationConfig.Set.Request.Params(
                taskId = TaskId.of("task-1"),
                pushNotificationConfig = PushNotificationConfig(url = Uri.of("https://example1.com/webhook"))
            )
        )
        configs.set(
            A2APushNotificationConfig.Set.Request.Params(
                taskId = TaskId.of("task-1"),
                pushNotificationConfig = PushNotificationConfig(url = Uri.of("https://example2.com/webhook"))
            )
        )
        val protocol =
            A2AProtocol(agentCard, { MessageResponse.Task(emptySequence()) }, pushNotificationConfigs = configs)

        val jsonRequest = A2AJson.asFormatString(
            A2APushNotificationConfig.List.Request(
                A2APushNotificationConfig.List.Request.Params(taskId = TaskId.of("task-1")),
                "1"
            )
        )

        val result = protocol(Request(POST, "/").body(jsonRequest))
        val response = result.valueOrNull()!!

        assertThat(response is A2AProtocolResponse.Single, equalTo(true))
        val singleResponse = response as A2AProtocolResponse.Single
        val json = A2AJson.asFormatString(singleResponse.response)
        assertThat(json.contains("\"configs\""), equalTo(true))
    }

    @Test
    fun `pushNotificationConfig delete removes config`() {
        val configs = pushNotificationConfigs()
        val created = configs.set(
            A2APushNotificationConfig.Set.Request.Params(
                taskId = TaskId.of("task-1"),
                pushNotificationConfig = PushNotificationConfig(url = Uri.of("https://example.com/webhook"))
            )
        )
        val protocol =
            A2AProtocol(agentCard, { MessageResponse.Task(emptySequence()) }, pushNotificationConfigs = configs)

        val jsonRequest = A2AJson.asFormatString(
            A2APushNotificationConfig.Delete.Request(
                A2APushNotificationConfig.Delete.Request.Params(id = created.id),
                "1"
            )
        )

        val result = protocol(Request(POST, "/").body(jsonRequest))
        val response = result.valueOrNull()!!

        assertThat(response is A2AProtocolResponse.Single, equalTo(true))
        val singleResponse = response as A2AProtocolResponse.Single
        val json = A2AJson.asFormatString(singleResponse.response)
        assertThat(json.contains("\"id\":\"${created.id.value}\""), equalTo(true))
    }

    @Test
    fun `pushNotificationConfig delete returns error for non-existent config`() {
        val configs = pushNotificationConfigs()
        val protocol =
            A2AProtocol(agentCard, { MessageResponse.Task(emptySequence()) }, pushNotificationConfigs = configs)

        val jsonRequest = A2AJson.asFormatString(
            A2APushNotificationConfig.Delete.Request(
                A2APushNotificationConfig.Delete.Request.Params(
                    id = PushNotificationConfigId.of("non-existent")
                ),
                "1"
            )
        )

        val result = protocol(Request(POST, "/").body(jsonRequest))
        val response = result.valueOrNull()!!

        assertThat(response is A2AProtocolResponse.Single, equalTo(true))
        val singleResponse = response as A2AProtocolResponse.Single
        val json = A2AJson.asFormatString(singleResponse.response)
        assertThat(json.contains("\"error\""), equalTo(true))
    }
}
