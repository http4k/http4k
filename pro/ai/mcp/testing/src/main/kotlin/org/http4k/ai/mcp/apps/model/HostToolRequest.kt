package org.http4k.ai.mcp.apps.model

import org.http4k.ai.model.ToolName
import org.http4k.core.Uri
import se.ansman.kotshi.JsonSerializable

@JsonSerializable
data class HostToolRequest(
    val serverId: Uri,
    val name: ToolName,
    val arguments: Map<String, String>
)

