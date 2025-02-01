package org.http4k.mcp.model

import se.ansman.kotshi.JsonSerializable

@JsonSerializable
data class CompletionArgument(val name: String, val value: String)
