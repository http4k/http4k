/*
 * Copyright (c) 2025-present http4k Ltd. All rights reserved.
 * Licensed under the http4k Commercial License: https://http4k.org/commercial-license
 */
package org.http4k.ai.mcp.stateless.server.capability.extension

import org.http4k.ai.mcp.stateless.ResourceHandler
import org.http4k.ai.mcp.stateless.ResourceRequest
import org.http4k.ai.mcp.stateless.ResourceResponse
import org.http4k.ai.mcp.stateless.ToolResponse
import org.http4k.ai.mcp.stateless.model.Content
import org.http4k.ai.mcp.stateless.model.Meta
import org.http4k.ai.mcp.stateless.model.Resource
import org.http4k.ai.mcp.stateless.model.Resource.Static
import org.http4k.ai.mcp.stateless.model.ResourceName
import org.http4k.ai.mcp.stateless.model.Tool
import org.http4k.ai.mcp.stateless.model.apps.McpAppMeta
import org.http4k.ai.mcp.stateless.model.apps.McpAppResourceMeta
import org.http4k.ai.mcp.stateless.model.apps.McpAppVisibility
import org.http4k.ai.mcp.stateless.model.apps.McpApps.MIME_TYPE
import org.http4k.ai.mcp.stateless.server.capability.ServerCapability
import org.http4k.ai.mcp.stateless.server.capability.capabilities
import org.http4k.ai.mcp.stateless.util.auto
import org.http4k.connect.model.MimeType
import org.http4k.core.Uri
import org.http4k.lens.stateless.MetaKey
import org.http4k.routing.stateless.bind

/**
 * Creates a combined Tool and Resource capability for MCP Apps.
 * The Tool triggers UI display, and the Resource serves the UI content.
 */
fun RenderMcpApp(
    name: String,
    description: String,
    uiUri: Uri = name.toFriendlyUiUri(),
    capabilities: List<ServerCapability> = emptyList(),
    toolVisibility: List<McpAppVisibility>? = null,
    mimeType: MimeType = MIME_TYPE,
    resourceHandler: ResourceHandler,
) = capabilities(
    listOf(
        Tool(name, description, meta = Meta(MetaKey.auto(McpAppMeta).toLens() of McpAppMeta(uiUri, toolVisibility)))
            bind { ToolResponse.Ok(listOf()) },
        Static(uiUri, ResourceName.of(name), description, mimeType) bind resourceHandler
    ) + capabilities
)

/**
 * Creates a combined Tool and Resource capability for MCP Apps.
 * The Tool triggers UI display, and the Resource serves the UI content.
 */
fun RenderMcpApp(
    name: String,
    description: String,
    uiUri: Uri = name.toFriendlyUiUri(),
    meta: McpAppResourceMeta = McpAppResourceMeta(),
    toolVisibility: List<McpAppVisibility>? = null,
    mimeType: MimeType = MIME_TYPE,
    capabilities: List<ServerCapability> = emptyList(),
    resourceHandler: (ResourceRequest) -> String
) = RenderMcpApp(name, description, uiUri, capabilities, toolVisibility, mimeType) {
    ResourceResponse.Ok(Resource.Content.Text(resourceHandler(it), uiUri, mimeType, Content.Meta(ui = meta)))
}

/**
 * Creates a combined Tool and Resource capability for MCP Apps.
 * The Tool triggers UI display, and the Resource serves the UI content.
 */
fun RenderMcpApp(
    name: String,
    description: String,
    uiUri: Uri = name.toFriendlyUiUri(),
    capabilities: List<ServerCapability> = emptyList(),
    toolVisibility: List<McpAppVisibility>? = null,
    resourceHandler: McpAppViewModelResourceHandler
) = RenderMcpApp(
    name,
    description,
    uiUri,
    capabilities,
    toolVisibility,
    resourceHandler.mimeType,
    resourceHandler
)

private fun String.toFriendlyUiUri(): Uri = Uri.of("ui://${filter { it.isLetterOrDigit() }}")
