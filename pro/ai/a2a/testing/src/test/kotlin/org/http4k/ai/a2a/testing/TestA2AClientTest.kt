package org.http4k.ai.a2a.testing

import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.equalTo
import dev.forkhandles.result4k.valueOrNull
import org.http4k.ai.a2a.model.AgentCard
import org.http4k.ai.a2a.model.AgentCapabilities
import org.http4k.ai.a2a.model.ContextId
import org.http4k.ai.a2a.model.Message
import org.http4k.ai.a2a.model.Part
import org.http4k.ai.a2a.model.Task
import org.http4k.ai.a2a.model.TaskId
import org.http4k.ai.a2a.model.TaskState
import org.http4k.ai.a2a.model.TaskStatus
import org.http4k.ai.a2a.protocol.messages.A2AMessage
import org.http4k.ai.a2a.server.http.a2aHttp
import org.http4k.ai.a2a.server.protocol.A2AProtocol
import org.http4k.ai.a2a.server.protocol.MessageHandler
import org.http4k.ai.a2a.server.protocol.MessageResponse
import org.http4k.ai.model.Role
import org.http4k.core.Uri
import org.junit.jupiter.api.Test
import java.util.UUID

class TestA2AClientTest {

    private val agentCard = AgentCard(
        name = "Test Agent",
        url = Uri.of("http://localhost:8080"),
        version = "1.0.0",
        capabilities = AgentCapabilities(streaming = false)
    )

    private val messageHandler: MessageHandler = { request ->
        val taskId = TaskId.of(UUID.randomUUID().toString())
        val contextId = ContextId.of(UUID.randomUUID().toString())

        MessageResponse.Task(
            sequenceOf(
                Task(
                    id = taskId,
                    contextId = contextId,
                    status = TaskStatus(state = TaskState.completed),
                    history = listOf(request.message)
                )
            )
        )
    }

    private val protocol = A2AProtocol(agentCard, messageHandler)
    private val server = a2aHttp(protocol)
    private val client = server.testA2AClient()

    @Test
    fun `can get agent card`() {
        val result = client.agentCard()
        val card = result.valueOrNull()!!
        assertThat(card.name, equalTo("Test Agent"))
        assertThat(card.version, equalTo("1.0.0"))
    }

    @Test
    fun `can send message and receive task`() {
        val message = Message(
            role = Role.User,
            parts = listOf(Part.Text("Hello, agent!"))
        )

        val result = client.message(message)
        val response = result.valueOrNull()!!

        assertThat(response is A2AMessage.Send.Response.Task, equalTo(true))
        val taskResponse = response as A2AMessage.Send.Response.Task
        assertThat(taskResponse.task.status.state, equalTo(TaskState.completed))
    }

}
