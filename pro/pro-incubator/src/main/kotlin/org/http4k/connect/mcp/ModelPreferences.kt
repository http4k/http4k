package org.http4k.connect.mcp

data class ModelPreferences(
    val hints: List<ModelHint>? = null,
    val costPriority: Double? = null,
    val speedPriority: Double? = null,
    val intelligencePriority: Double? = null
)

