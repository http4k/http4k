/*
 * Copyright (c) 2025-present http4k Ltd. All rights reserved.
 * Licensed under the http4k Commercial License: https://http4k.org/commercial-license
 */
package org.http4k.ai.mcp.a2a

import com.natpryce.hamkrest.absent
import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.containsSubstring
import com.natpryce.hamkrest.equalTo
import com.natpryce.hamkrest.isA
import com.natpryce.hamkrest.present
import org.http4k.ai.a2a.MessageHandler
import org.http4k.ai.a2a.client.A2AClient
import org.http4k.ai.a2a.client.HttpA2AClient
import org.http4k.ai.a2a.client.testA2AJsonRpcClient
import org.http4k.ai.a2a.model.A2ARole.ROLE_AGENT
import org.http4k.ai.a2a.model.A2ARole.ROLE_USER
import org.http4k.ai.a2a.model.AgentCard
import org.http4k.ai.a2a.model.AgentSkill
import org.http4k.ai.a2a.model.ContextId
import org.http4k.ai.a2a.model.Message
import org.http4k.ai.a2a.model.MessageId
import org.http4k.ai.a2a.model.Part
import org.http4k.ai.a2a.model.SkillId
import org.http4k.ai.a2a.model.Task
import org.http4k.ai.a2a.model.TaskId
import org.http4k.ai.a2a.model.TaskState.TASK_STATE_CANCELED
import org.http4k.ai.a2a.model.TaskState.TASK_STATE_COMPLETED
import org.http4k.ai.a2a.model.TaskState.TASK_STATE_WORKING
import org.http4k.ai.a2a.model.TaskStatus
import org.http4k.ai.a2a.model.Version
import org.http4k.ai.a2a.server.storage.PushNotificationConfigStorage
import org.http4k.ai.a2a.server.storage.TaskStorage
import org.http4k.ai.mcp.ToolRequest
import org.http4k.ai.mcp.ToolResponse
import org.http4k.ai.mcp.a2a.capabilities.CancelTask
import org.http4k.ai.mcp.a2a.capabilities.GetTask
import org.http4k.ai.mcp.a2a.capabilities.ListTasks
import org.http4k.ai.mcp.a2a.capabilities.SendMessage
import org.http4k.ai.mcp.a2a.capabilities.sendMessageOutput
import org.http4k.ai.mcp.a2a.capabilities.taskOutput
import org.http4k.ai.mcp.a2a.capabilities.taskPageOutput
import org.http4k.ai.mcp.server.capability.ToolCapability
import org.http4k.core.Method.POST
import org.http4k.core.Request
import org.http4k.lens.Header
import org.http4k.core.Response
import org.http4k.core.Status.Companion.NOT_FOUND
import org.http4k.core.Uri
import org.http4k.routing.a2aJsonRpc
import org.http4k.testing.toHttpHandler
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import java.util.Random

class McpA2aBridgeTest {

    private fun testA2A(
        card: AgentCard,
        tasks: TaskStorage = TaskStorage.InMemory(),
        messageHandler: MessageHandler = { error("not implemented: $it") }
    ): (Request) -> A2AClient {
        val client = a2aJsonRpc(
            agentCard = card,
            tasks = tasks,
            pushNotifications = PushNotificationConfigStorage.InMemory(),
            messageHandler = messageHandler
        ).testA2AJsonRpcClient()
        return { client }
    }

    private val card = AgentCard(
        name = "Test Agent",
        version = Version.of("1.0.0"),
        description = "agent for tests",
        skills = listOf(
            AgentSkill(
                id = SkillId.of("translate"),
                name = "Translate",
                description = "translates text",
                examples = listOf("translate hello")
            ),
            AgentSkill(
                id = SkillId.of("summarize"),
                name = "Summarize",
                description = "summarises text"
            )
        )
    )

    private val random = Random(0)

    @Test
    fun `bridge exposes the four expected tools`() {
        val server = mcpA2aBridge(testA2A(card), random)
        assertThat(
            server.toList().map { it.name },
            equalTo(listOf("send_message", "get_task", "cancel_task", "list_tasks"))
        )
    }

    @Test
    fun `bridge throws when agent card fetch fails`() {
        val failingClient = HttpA2AClient(Uri.of(""), http = { Response(NOT_FOUND) })
        assertThrows<IllegalStateException> { mcpA2aBridge(failingClient, random) }
    }

    @Test
    fun `uri overload wires up an HttpA2AClient against the configured endpoint`() {
        val server = a2aJsonRpc(
            agentCard = card,
            tasks = TaskStorage.InMemory(),
            pushNotifications = PushNotificationConfigStorage.InMemory(),
            messageHandler = { error("not used") }
        )

        val bridge = mcpA2aBridge(
            baseUri = Uri.of(""),
            http = server.toHttpHandler(),
            random = random,
            authLens = Header.optional("Authorization")
        )

        assertThat(
            bridge.toList().map { it.name },
            equalTo(listOf("send_message", "get_task", "cancel_task", "list_tasks"))
        )
    }

    @Test
    fun `uri overload forwards inbound Authorization header to A2A`() {
        var receivedAuth: String? = null
        val server = a2aJsonRpc(
            agentCard = card,
            tasks = TaskStorage.InMemory(),
            pushNotifications = PushNotificationConfigStorage.InMemory(),
            messageHandler = {
                receivedAuth = it.http.header("Authorization")
                Message(MessageId.of("m1"), ROLE_AGENT, listOf(Part.Text("ok")))
            }
        )

        val bridge = mcpA2aBridge(
            baseUri = Uri.of(""),
            http = server.toHttpHandler(),
            random = random,
            authLens = Header.optional("Authorization")
        )
        val sendMessage = bridge.toList().first { it.name == "send_message" } as ToolCapability

        sendMessage(
            ToolRequest(
                args = mapOf("message" to "hi"),
                connectRequest = Request(POST, "").header("Authorization", "Bearer xyz")
            )
        )

        assertThat(receivedAuth, equalTo("Bearer xyz"))
    }

    @Test
    fun `send_message forwards user text and contextId to A2A`() {
        var received: Message? = null
        val client = testA2A(card, messageHandler = {
            received = it.message
            Message(MessageId.of("m1"), ROLE_AGENT, listOf(Part.Text("response")))
        })

        SendMessage(card, client, random)(ToolRequest(args = mapOf("message" to "hi", "contextId" to "ctx-1")))

        val sent = received!!
        assertThat(sent.role, equalTo(ROLE_USER))
        assertThat(sent.parts, equalTo(listOf<Part>(Part.Text("hi"))))
        assertThat(sent.contextId, equalTo(ContextId.of("ctx-1")))
    }

    @Test
    fun `send_message maps Message response to structured content`() {
        val client = testA2A(card, messageHandler = {
            Message(MessageId.of("m1"), ROLE_AGENT, listOf(Part.Text("hello back")))
        })

        val response =
            SendMessage(card, client, random)(ToolRequest(args = mapOf("message" to "hi"))) as ToolResponse.Ok
        val result = sendMessageOutput(response)

        assertThat(result.task, absent())
        assertThat(result.message?.parts, equalTo(listOf<Part>(Part.Text("hello back"))))
    }

    @Test
    fun `send_message maps Task response to structured content`() {
        val tasks = TaskStorage.InMemory()
        val client = testA2A(card, tasks = tasks, messageHandler = {
            val task = Task(
                id = TaskId.of("t1"),
                status = TaskStatus(state = TASK_STATE_COMPLETED),
                contextId = ContextId.of("ctx-1"),
                history = listOf(it.message)
            )
            tasks.store(task)
            task
        })

        val response =
            SendMessage(card, client, random)(ToolRequest(args = mapOf("message" to "go"))) as ToolResponse.Ok
        val result = sendMessageOutput(response)

        assertThat(result.message, absent())
        val task = result.task!!
        assertThat(task.id, equalTo(TaskId.of("t1")))
        assertThat(task.status.state, equalTo(TASK_STATE_COMPLETED))
    }

    @Test
    fun `send_message description includes agent metadata and skill catalog`() {
        val description = SendMessage(card, testA2A(card), random).toTool().description

        assertThat(description, containsSubstring("Test Agent"))
        assertThat(description, containsSubstring("agent for tests"))
        assertThat(description, containsSubstring("Translate"))
        assertThat(description, containsSubstring("translates text"))
        assertThat(description, containsSubstring("translate hello"))
        assertThat(description, containsSubstring("Summarize"))
    }

    @Test
    fun `get_task returns Task as structured content`() {
        val tasks = TaskStorage.InMemory()
        tasks.store(
            Task(
                id = TaskId.of("t1"),
                status = TaskStatus(state = TASK_STATE_COMPLETED),
                contextId = ContextId.of("ctx-1")
            )
        )

        val response =
            GetTask(testA2A(card, tasks = tasks))(ToolRequest(args = mapOf("taskId" to "t1"))) as ToolResponse.Ok

        assertThat(taskOutput(response).id, equalTo(TaskId.of("t1")))
    }

    @Test
    fun `get_task returns Error for unknown task`() {
        val response = GetTask(testA2A(card))(ToolRequest(args = mapOf("taskId" to "unknown")))
        assertThat(response, isA<ToolResponse.Error>())
    }

    @Test
    fun `cancel_task transitions task to cancelled state`() {
        val tasks = TaskStorage.InMemory()
        tasks.store(
            Task(
                id = TaskId.of("t1"),
                status = TaskStatus(state = TASK_STATE_WORKING),
                contextId = ContextId.of("ctx-1")
            )
        )

        val response =
            CancelTask(testA2A(card, tasks = tasks))(ToolRequest(args = mapOf("taskId" to "t1"))) as ToolResponse.Ok

        assertThat(taskOutput(response).status.state, equalTo(TASK_STATE_CANCELED))
    }

    @Test
    fun `cancel_task returns Error for unknown task`() {
        val response = CancelTask(testA2A(card))(ToolRequest(args = mapOf("taskId" to "unknown")))
        assertThat(response, isA<ToolResponse.Error>())
    }

    @Test
    fun `list_tasks returns TaskPage as structured content`() {
        val tasks = TaskStorage.InMemory()
        tasks.store(Task(TaskId.of("t1"), TaskStatus(state = TASK_STATE_COMPLETED), ContextId.of("c")))
        tasks.store(Task(TaskId.of("t2"), TaskStatus(state = TASK_STATE_WORKING), ContextId.of("c")))

        val response = ListTasks(testA2A(card, tasks = tasks))(ToolRequest(args = emptyMap())) as ToolResponse.Ok

        assertThat(taskPageOutput(response).tasks.size, equalTo(2))
    }

    @Test
    fun `list_tasks filters by status`() {
        val tasks = TaskStorage.InMemory()
        tasks.store(Task(TaskId.of("t1"), TaskStatus(state = TASK_STATE_COMPLETED), ContextId.of("c")))
        tasks.store(Task(TaskId.of("t2"), TaskStatus(state = TASK_STATE_WORKING), ContextId.of("c")))

        val response = ListTasks(testA2A(card, tasks = tasks))(
            ToolRequest(args = mapOf("status" to "TASK_STATE_WORKING"))
        ) as ToolResponse.Ok

        assertThat(taskPageOutput(response).tasks.map { it.id }, equalTo(listOf(TaskId.of("t2"))))
    }

    @Test
    fun `all tools publish an output schema`() {
        val client = testA2A(card)
        listOf(
            SendMessage(card, client, random),
            GetTask(client),
            CancelTask(client),
            ListTasks(client)
        ).forEach { assertThat(it.toTool().outputSchema, present()) }
    }
}
