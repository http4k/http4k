package org.http4k.mcp.model

import se.ansman.kotshi.JsonSerializable

@JsonSerializable
data class ModelPreferences(
    val hints: List<ModelHint>? = null,
    val costPriority: CostPriority? = null,
    val speedPriority: SpeedPriority? = null,
    val intelligencePriority: IntelligencePriority? = null
)

