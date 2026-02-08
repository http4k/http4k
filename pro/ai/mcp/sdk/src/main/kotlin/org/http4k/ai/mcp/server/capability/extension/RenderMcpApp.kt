package org.http4k.ai.mcp.server.capability.extension

import org.http4k.ai.mcp.ResourceRequest
import org.http4k.ai.mcp.ResourceResponse
import org.http4k.ai.mcp.ToolResponse
import org.http4k.ai.mcp.model.Content
import org.http4k.ai.mcp.model.Meta
import org.http4k.ai.mcp.model.Resource.Content.Text
import org.http4k.ai.mcp.model.Resource.Static
import org.http4k.ai.mcp.model.ResourceName
import org.http4k.ai.mcp.model.Tool
import org.http4k.ai.mcp.model.apps.McpAppMeta
import org.http4k.ai.mcp.model.apps.McpAppResourceMeta
import org.http4k.ai.mcp.model.apps.McpAppVisibility
import org.http4k.ai.mcp.model.apps.McpApps
import org.http4k.ai.mcp.server.capability.CapabilityPack
import org.http4k.core.Uri
import org.http4k.routing.bind

/**
 * Creates a combined Tool and Resource capability for MCP Apps.
 * The Tool triggers UI display, and the Resource serves the HTML content.
 */
fun RenderMcpApp(
    name: String,
    description: String,
    uri: Uri,
    meta: McpAppResourceMeta = McpAppResourceMeta(),
    visibility: List<McpAppVisibility>? = null,
    mcpAppHandler: McpAppHandler
) = CapabilityPack(
    Tool(
        name = name,
        description = description,
        meta = Meta(ui = McpAppMeta(uri, visibility))
    ) bind { ToolResponse.Ok(listOf()) }, Static(uri, ResourceName.of(name), description, McpApps.MIME_TYPE) bind {
        ResourceResponse(Text(mcpAppHandler(it), it.uri, McpApps.MIME_TYPE, Content.Meta(ui = meta)))
    })

typealias McpAppHandler = (ResourceRequest) -> String
