package org.http4k.connect.mcp

data class Completion(val values: List<String>, val total: Int? = null, val hasMore: Boolean? = null)
