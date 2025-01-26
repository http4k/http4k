package org.http4k.mcp.model

import se.ansman.kotshi.JsonSerializable

@JsonSerializable
data class Progress(val progress: Int, val total: Double?, val progressToken: ProgressToken)

