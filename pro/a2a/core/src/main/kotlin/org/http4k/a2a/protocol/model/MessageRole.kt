package org.http4k.a2a.protocol.model

import se.ansman.kotshi.JsonSerializable

@JsonSerializable
enum class MessageRole {
    agent,
    user
}
