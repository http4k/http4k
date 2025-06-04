package org.http4k.ai.mcp.model

import org.http4k.ai.model.Role
import se.ansman.kotshi.JsonSerializable

@JsonSerializable
data class Annotations(val audience: List<Role>, val priority: Priority)
