package org.http4k.ai.a2a.server.capability

import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.equalTo
import org.http4k.ai.a2a.model.ContextId
import org.http4k.ai.a2a.model.Message
import org.http4k.ai.a2a.model.Part
import org.http4k.ai.a2a.model.Task
import org.http4k.ai.a2a.model.TaskId
import org.http4k.ai.a2a.model.TaskState
import org.http4k.ai.a2a.model.TaskStatus
import org.http4k.ai.a2a.protocol.messages.A2AMessage
import org.http4k.ai.a2a.server.protocol.MessageHandler
import org.http4k.ai.a2a.server.protocol.MessageResponse
import org.http4k.ai.model.Role
import org.http4k.core.Method.POST
import org.http4k.core.Request
import org.junit.jupiter.api.Test

class ServerMessagesTest {

    private fun aTask(id: String = "task-1") = Task(
        id = TaskId.of(id),
        contextId = ContextId.of("ctx-1"),
        status = TaskStatus(state = TaskState.completed)
    )

    private fun aMessage() = Message(role = Role.Assistant, parts = listOf(Part.Text("Hello")))

    private val httpRequest = Request(POST, "/")

    @Test
    fun `send returns TaskResponse when handler yields Task`() {
        val task = aTask()
        val handler: MessageHandler = { MessageResponse.Task(task) }
        val messages = ServerMessages(handler)

        val request = A2AMessage.Send.Request(Message(role = Role.User, parts = listOf(Part.Text("Hi"))))
        val response = messages.send(request, httpRequest)

        assertThat(response, equalTo(A2AMessage.Send.Response.Task(task) as A2AMessage.Send.Response))
    }

    @Test
    fun `send returns MessageResponse when handler yields Message`() {
        val message = aMessage()
        val handler: MessageHandler = { MessageResponse.Message(message) }
        val messages = ServerMessages(handler)

        val request = A2AMessage.Send.Request(Message(role = Role.User, parts = listOf(Part.Text("Hi"))))
        val response = messages.send(request, httpRequest)

        assertThat(response, equalTo(A2AMessage.Send.Response.Message(message) as A2AMessage.Send.Response))
    }

    @Test
    fun `stream returns sequence of TaskResponses when handler yields Task`() {
        val task1 = aTask("task-1")
        val task2 = aTask("task-2")
        val handler: MessageHandler = { MessageResponse.Task(sequenceOf(task1, task2)) }
        val messages = ServerMessages(handler)

        val request = A2AMessage.Stream.Request(Message(role = Role.User, parts = listOf(Part.Text("Hi"))))
        val responses = messages.stream(request, httpRequest).toList()

        assertThat(responses.size, equalTo(2))
        assertThat(responses[0], equalTo(A2AMessage.Send.Response.Task(task1) as A2AMessage.Send.Response))
        assertThat(responses[1], equalTo(A2AMessage.Send.Response.Task(task2) as A2AMessage.Send.Response))
    }

    @Test
    fun `stream returns single MessageResponse when handler yields Message`() {
        val message = aMessage()
        val handler: MessageHandler = { MessageResponse.Message(message) }
        val messages = ServerMessages(handler)

        val request = A2AMessage.Stream.Request(Message(role = Role.User, parts = listOf(Part.Text("Hi"))))
        val responses = messages.stream(request, httpRequest).toList()

        assertThat(responses.size, equalTo(1))
        assertThat(responses[0], equalTo(A2AMessage.Send.Response.Message(message) as A2AMessage.Send.Response))
    }
}
