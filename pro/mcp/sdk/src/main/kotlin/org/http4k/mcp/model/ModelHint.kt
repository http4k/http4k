package org.http4k.mcp.model

import se.ansman.kotshi.JsonSerializable

@JsonSerializable
data class ModelHint(val name: ModelIdentifier)
