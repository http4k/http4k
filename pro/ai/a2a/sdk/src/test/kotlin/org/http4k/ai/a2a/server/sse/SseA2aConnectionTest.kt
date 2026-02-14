package org.http4k.ai.a2a.server.sse

import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.equalTo
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
import org.http4k.ai.a2a.server.protocol.A2AProtocol
import org.http4k.ai.a2a.server.protocol.MessageHandler
import org.http4k.ai.a2a.server.protocol.MessageResponse
import org.http4k.ai.a2a.util.A2AJson
import org.http4k.ai.model.Role
import org.http4k.core.Method.POST
import org.http4k.core.Request
import org.http4k.core.Status.Companion.OK
import org.http4k.core.Uri
import org.http4k.sse.SseMessage
import org.http4k.testing.testSseClient
import org.junit.jupiter.api.Test
import java.util.UUID

class SseA2aConnectionTest {

    private val agentCard = AgentCard(
        name = "Streaming Agent",
        url = Uri.of("http://localhost:8080"),
        version = "1.0.0",
        capabilities = AgentCapabilities(streaming = true)
    )

    @Test
    fun `streams task updates via SSE`() {
        val taskId = TaskId.of(UUID.randomUUID().toString())
        val contextId = ContextId.of(UUID.randomUUID().toString())

        val handler: MessageHandler = { request ->
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

        val protocol = A2AProtocol(agentCard, handler)
        val sseHandler = SseA2aConnection(protocol)

        val message = Message(role = Role.User, parts = listOf(Part.Text("Hello")))
        val jsonRequest = """
            {
                "jsonrpc": "2.0",
                "id": "1",
                "method": "message/stream",
                "params": ${A2AJson.asFormatString(A2AMessage.Stream.Request(message))}
            }
        """.trimIndent()

        val client = sseHandler.testSseClient(Request(POST, "/").body(jsonRequest))

        assertThat(client.status, equalTo(OK))

        val events = client.received().toList()
        assertThat(events.size, equalTo(2))

        val firstEvent = events[0] as SseMessage.Data
        assertThat(firstEvent.data.contains("working"), equalTo(true))

        val secondEvent = events[1] as SseMessage.Data
        assertThat(secondEvent.data.contains("completed"), equalTo(true))
    }

    @Test
    fun `streams message response as single SSE event`() {
        val handler: MessageHandler = { request ->
            MessageResponse.Message(
                Message(
                    role = Role.Assistant,
                    parts = listOf(Part.Text("Response to: ${request.message.parts}"))
                )
            )
        }

        val protocol = A2AProtocol(agentCard, handler)
        val sseHandler = SseA2aConnection(protocol)

        val message = Message(role = Role.User, parts = listOf(Part.Text("Hello")))
        val jsonRequest = """
            {
                "jsonrpc": "2.0",
                "id": "1",
                "method": "message/stream",
                "params": ${A2AJson.asFormatString(A2AMessage.Stream.Request(message))}
            }
        """.trimIndent()

        val client = sseHandler.testSseClient(Request(POST, "/").body(jsonRequest))

        assertThat(client.status, equalTo(OK))

        val events = client.received().toList()
        assertThat(events.size, equalTo(1))

        val event = events[0] as SseMessage.Data
        assertThat(event.data.contains("assistant"), equalTo(true))
    }
}
