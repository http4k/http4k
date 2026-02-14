package org.http4k.ai.a2a.server.protocol

import org.http4k.ai.a2a.protocol.messages.A2AMessage
import org.http4k.core.Request

interface Messages {
    fun send(request: A2AMessage.Send.Request, http: Request): A2AMessage.Send.Response
    fun stream(request: A2AMessage.Stream.Request, http: Request): Sequence<A2AMessage.Send.Response>
}
