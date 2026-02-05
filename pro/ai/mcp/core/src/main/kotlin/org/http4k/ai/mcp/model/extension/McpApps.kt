package org.http4k.ai.mcp.model.extension

import org.http4k.ai.mcp.protocol.McpExtension
import org.http4k.connect.model.MimeType

object McpApps : McpExtension {
    override val name = "io.modelcontextprotocol/ui"
    val MIME_TYPE = MimeType.of("text/html;profile=mcp-app")
    override val config = mapOf(name to MIME_TYPE)
}
