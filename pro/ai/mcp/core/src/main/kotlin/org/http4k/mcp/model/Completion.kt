package org.http4k.mcp.model

import se.ansman.kotshi.JsonSerializable

@JsonSerializable
data class Completion(val values: List<String>, val total: Int? = null, val hasMore: Boolean? = null) {
    constructor(vararg values: String, total: Int? = null, hasMore: Boolean? = null) : this(
        values.toList(), total,
        hasMore
    )
}
