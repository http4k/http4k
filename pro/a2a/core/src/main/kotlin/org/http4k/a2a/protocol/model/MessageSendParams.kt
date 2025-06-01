package org.http4k.a2a.protocol.model

import se.ansman.kotshi.JsonSerializable

@JsonSerializable
data class MessageSendParams(
    val message: Message,
    val configuration: MessageSendConfiguration? = null,
    val metadata: Metadata? = null
)
