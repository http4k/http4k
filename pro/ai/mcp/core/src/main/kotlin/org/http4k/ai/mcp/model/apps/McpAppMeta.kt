package org.http4k.ai.mcp.model.apps

import org.http4k.core.Uri
import se.ansman.kotshi.JsonSerializable

@JsonSerializable
data class McpAppMeta(
    val resourceUri: Uri? = null,
    val visibility: List<McpAppVisibility>? = null
)
