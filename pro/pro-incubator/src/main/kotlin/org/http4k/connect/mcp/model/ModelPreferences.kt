package org.http4k.connect.mcp.model

import org.http4k.connect.mcp.SpeedPriority

data class ModelPreferences(
    val hints: List<ModelHint>? = null,
    val costPriority: CostPriority? = null,
    val speedPriority: SpeedPriority? = null,
    val intelligencePriority: IntelligencePriority? = null
)

