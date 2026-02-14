package org.http4k.ai.a2a.server.protocol

import org.http4k.ai.a2a.model.Message
import org.http4k.core.Request

data class MessageRequest(
    val message: Message,
    val http: Request
)
