package org.http4k.ai.a2a.server.http

import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.containsSubstring
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
import org.http4k.core.ContentType.Companion.TEXT_EVENT_STREAM
import org.http4k.core.Method.POST
import org.http4k.core.Request
import org.http4k.core.Status.Companion.OK
import org.http4k.core.Uri
import org.junit.jupiter.api.Test
import java.util.UUID

class HttpA2AStreamingTest {

    private val agentCard = AgentCard(
        name = "Streaming Agent",
        url = Uri.of("http://localhost:8080"),
        version = "1.0.0",
        capabilities = AgentCapabilities(streaming = true)
    )

    @Test
    fun `message send returns JSON response`() {
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
        val httpHandler = a2aHttp(protocol)

        val message = Message(role = Role.User, parts = listOf(Part.Text("Hello")))
        val jsonRequest = """
            {
                "jsonrpc": "2.0",
                "id": "1",
                "method": "message/send",
                "params": ${A2AJson.asFormatString(A2AMessage.Send.Request(message))}
            }
        """.trimIndent()

        val response = httpHandler(Request(POST, "/").body(jsonRequest))

        assertThat(response.status, equalTo(OK))
        assertThat(response.header("Content-Type")!!, containsSubstring("application/json"))
        assertThat(response.bodyString(), containsSubstring("jsonrpc"))
        assertThat(response.bodyString(), containsSubstring("result"))
    }

    @Test
    fun `message stream returns SSE response`() {
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
        val httpHandler = a2aHttp(protocol)

        val message = Message(role = Role.User, parts = listOf(Part.Text("Hello")))
        val jsonRequest = """
            {
                "jsonrpc": "2.0",
                "id": "1",
                "method": "message/stream",
                "params": ${A2AJson.asFormatString(A2AMessage.Stream.Request(message))}
            }
        """.trimIndent()

        val response = httpHandler(Request(POST, "/").body(jsonRequest))

        assertThat(response.status, equalTo(OK))
        assertThat(response.header("Content-Type")!!, containsSubstring(TEXT_EVENT_STREAM.value))

        val body = response.bodyString()
        assertThat(body, containsSubstring("data:"))
        assertThat(body, containsSubstring("working"))
        assertThat(body, containsSubstring("completed"))
    }
}
