package org.http4k.mcp.model

import org.http4k.ai.model.ModelName
import se.ansman.kotshi.JsonSerializable

@JsonSerializable
data class
ModelHint(val name: ModelName)
