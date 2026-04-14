/*
 * Copyright (c) 2025-present http4k Ltd. All rights reserved.
 * Licensed under the http4k Commercial License: https://http4k.org/commercial-license
 */
package org.http4k.ai.mcp.server.capability.extension

import org.http4k.ai.mcp.ResourceHandler
import org.http4k.ai.mcp.ResourceRequest
import org.http4k.ai.mcp.ResourceResponse
import org.http4k.ai.mcp.ToolResponse
import org.http4k.ai.mcp.model.Content
import org.http4k.ai.mcp.model.Meta
import org.http4k.ai.mcp.model.Resource
import org.http4k.ai.mcp.model.Resource.Static
import org.http4k.ai.mcp.model.ResourceName
import org.http4k.ai.mcp.model.Tool
import org.http4k.ai.mcp.model.apps.McpAppMeta
import org.http4k.ai.mcp.model.apps.McpAppResourceMeta
import org.http4k.ai.mcp.model.apps.McpAppVisibility
import org.http4k.ai.mcp.model.apps.McpApps.MIME_TYPE
import org.http4k.ai.mcp.server.capability.ServerCapability
import org.http4k.ai.mcp.server.capability.capabilities
import org.http4k.ai.mcp.util.auto
import org.http4k.connect.model.MimeType
import org.http4k.core.Uri
import org.http4k.lens.MetaKey
import org.http4k.routing.bind


/**
 * Creates a combined Tool and Resource capability for MCP Apps.
 * The Tool triggers UI display, and the Resource serves the UI content.
 */
fun RenderMcpApp(
    name: String,
    description: String,
    uiUri: Uri,
    extraCapabilities: List<ServerCapability>,
    toolVisibility: List<McpAppVisibility>? = null,
    mimeType: MimeType = MIME_TYPE,
    resourceHandler: ResourceHandler,
) = capabilities(
    listOf(
        Tool(
            name,
            description,
            meta = Meta(MetaKey.auto(McpAppMeta).toLens() of McpAppMeta(uiUri, toolVisibility))
        ) bind {
            ToolResponse.Ok(listOf())
        },
        Static(uiUri, ResourceName.of(name), description, mimeType) bind resourceHandler
    ) + extraCapabilities
)

/**
 * Creates a combined Tool and Resource capability for MCP Apps.
 * The Tool triggers UI display, and the Resource serves the UI content.
 */
fun RenderMcpApp(
    name: String,
    description: String,
    uiUri: Uri,
    meta: McpAppResourceMeta = McpAppResourceMeta(),
    toolVisibility: List<McpAppVisibility>? = null,
    mimeType: MimeType = MIME_TYPE,
    extraCapabilities: List<ServerCapability> = emptyList(),
    resourceHandler: (ResourceRequest) -> String
) = RenderMcpApp(name, description, uiUri, extraCapabilities, toolVisibility, mimeType) {
    ResourceResponse.Ok(Resource.Content.Text(resourceHandler(it), uiUri, mimeType, Content.Meta(ui = meta)))
}
