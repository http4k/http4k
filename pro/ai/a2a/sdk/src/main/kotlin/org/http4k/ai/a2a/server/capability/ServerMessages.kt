package org.http4k.ai.a2a.server.capability

import org.http4k.ai.a2a.protocol.messages.A2AMessage
import org.http4k.ai.a2a.server.protocol.MessageHandler
import org.http4k.ai.a2a.server.protocol.MessageRequest
import org.http4k.ai.a2a.server.protocol.MessageResponse
import org.http4k.ai.a2a.server.protocol.Messages
import org.http4k.core.Request

class ServerMessages(private val handler: MessageHandler) : Messages {
    override fun send(request: A2AMessage.Send.Request, http: Request): A2AMessage.Send.Response {
        val response = handler(MessageRequest(request.message, http))
        return when (response) {
            is MessageResponse.Task -> A2AMessage.Send.Response.Task(response.tasks.first())
            is MessageResponse.Message -> A2AMessage.Send.Response.Message(response.message)
        }
    }

    override fun stream(request: A2AMessage.Stream.Request, http: Request): Sequence<A2AMessage.Send.Response> {
        val response = handler(MessageRequest(request.message, http))
        return when (response) {
            is MessageResponse.Task -> response.tasks.map { A2AMessage.Send.Response.Task(it) }
            is MessageResponse.Message -> sequenceOf(A2AMessage.Send.Response.Message(response.message))
        }
    }
}
