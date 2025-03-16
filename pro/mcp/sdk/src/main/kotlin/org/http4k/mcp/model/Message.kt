package org.http4k.mcp.model

import org.http4k.connect.model.Role
import se.ansman.kotshi.JsonSerializable

@JsonSerializable
data class Message(val role: Role, val content: Content)
