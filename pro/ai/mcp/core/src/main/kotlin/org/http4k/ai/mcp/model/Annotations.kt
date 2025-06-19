package org.http4k.ai.mcp.model

import org.http4k.ai.model.Role
import se.ansman.kotshi.JsonSerializable
import java.time.Instant

@JsonSerializable
data class Annotations(val audience: List<Role>? = null, val priority: Priority? = null, val lastModified: Instant? = null)
