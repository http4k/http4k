package org.http4k.ai.mcp.apps.model

import org.http4k.ai.model.ToolName
import org.http4k.core.Uri

data class ToolOption(
    val serverId: Uri,
    val serverName: String,
    val toolName: ToolName,
    val resourceUri: Uri
)
