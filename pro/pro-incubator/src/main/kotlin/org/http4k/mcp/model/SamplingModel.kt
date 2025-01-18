package org.http4k.mcp.model

interface SamplingModel {
    val name: ModelName
    fun score(model: ModelPreferences): Int
}
