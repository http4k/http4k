package org.http4k.mcp.model

import se.ansman.kotshi.JsonSerializable

@JsonSerializable
data class Message(val role: Role, val content: Content)
