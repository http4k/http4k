package org.http4k.ai.mcp.model

import se.ansman.kotshi.JsonSerializable

@JsonSerializable
data class CompletionArgument(val name: String, val value: String)
