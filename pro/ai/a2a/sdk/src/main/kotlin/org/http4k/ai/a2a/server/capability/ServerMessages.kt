/*
 * Copyright (c) 2025-present http4k Ltd. All rights reserved.
 * Licensed under the http4k Commercial License: https://http4k.org/commercial-license
 */
package org.http4k.ai.a2a.server.capability

import org.http4k.ai.a2a.protocol.messages.A2AMessage
import org.http4k.ai.a2a.MessageHandler
import org.http4k.ai.a2a.MessageRequest
import org.http4k.ai.a2a.MessageResponse
import org.http4k.ai.a2a.server.protocol.Messages
import org.http4k.core.Request

fun messages(handler: MessageHandler): Messages = ServerMessages(handler)

private class ServerMessages(private val handler: MessageHandler) : Messages {
    override fun send(params: A2AMessage.Send.Request.Params, http: Request): A2AMessage.Send.Response {
        val response = handler(MessageRequest(params.message, http))
        return when (response) {
            is MessageResponse.Task -> A2AMessage.Send.Response.Task(response.tasks.first())
            is MessageResponse.Message -> A2AMessage.Send.Response.Message(response.message)
        }
    }

    override fun stream(params: A2AMessage.Stream.Request.Params, http: Request): Sequence<A2AMessage.Send.Response> {
        val response = handler(MessageRequest(params.message, http))
        return when (response) {
            is MessageResponse.Task -> response.tasks.map { A2AMessage.Send.Response.Task(it) }
            is MessageResponse.Message -> sequenceOf(A2AMessage.Send.Response.Message(response.message))
        }
    }
}
