package org.http4k.mcp.model

import org.http4k.connect.model.ModelName
import se.ansman.kotshi.JsonSerializable

@JsonSerializable
data class
ModelHint(val name: ModelName)
