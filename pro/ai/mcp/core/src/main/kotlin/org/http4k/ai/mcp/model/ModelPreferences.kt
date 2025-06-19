package org.http4k.ai.mcp.model

import se.ansman.kotshi.JsonSerializable

@JsonSerializable
data class ModelPreferences(
    val hints: List<ModelHint>? = null,
    val costPriority: Priority? = null,
    val speedPriority: Priority? = null,
    val intelligencePriority: Priority? = null
)

