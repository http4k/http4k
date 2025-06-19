package org.http4k.ai.mcp.model

import se.ansman.kotshi.JsonSerializable

@JsonSerializable
data class CompletionContext(val arguments: Map<String, String> = emptyMap()) {
    constructor(vararg arguments: Pair<String, String>) : this(mapOf(*arguments))
}
