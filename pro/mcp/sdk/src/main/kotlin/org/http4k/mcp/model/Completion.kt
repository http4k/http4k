package org.http4k.mcp.model

import se.ansman.kotshi.JsonSerializable

@JsonSerializable
data class Completion(val values: List<String>, val total: Int? = null, val hasMore: Boolean? = null)
