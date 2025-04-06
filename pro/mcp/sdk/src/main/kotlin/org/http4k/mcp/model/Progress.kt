package org.http4k.mcp.model

import se.ansman.kotshi.JsonSerializable

@JsonSerializable
data class Progress(
    val progressToken: ProgressToken,
    val progress: Int,
    val total: Double? = null,
    val description: String? = null
)

