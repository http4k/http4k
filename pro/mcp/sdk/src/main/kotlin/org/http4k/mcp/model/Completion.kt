package org.http4k.mcp.model

data class Completion(val values: List<String>, val total: Int? = null, val hasMore: Boolean? = null)
