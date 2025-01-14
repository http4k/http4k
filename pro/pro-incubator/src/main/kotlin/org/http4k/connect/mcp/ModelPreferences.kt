package org.http4k.connect.mcp

data class ModelPreferences(
    val hints: List<ModelHint>? = null,
    val costPriority: CostPriority? = null,
    val speedPriority: SpeedPriority? = null,
    val intelligencePriority: IntelligencePriority? = null
)

