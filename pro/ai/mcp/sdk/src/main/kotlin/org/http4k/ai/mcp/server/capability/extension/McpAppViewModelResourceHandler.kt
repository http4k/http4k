/*
 * Copyright (c) 2025-present http4k Ltd. All rights reserved.
 * Licensed under the http4k Commercial License: https://http4k.org/commercial-license
 */
package org.http4k.ai.mcp.server.capability.extension

import org.http4k.ai.mcp.ResourceHandler
import org.http4k.ai.mcp.ResourceRequest
import org.http4k.ai.mcp.ResourceResponse
import org.http4k.ai.mcp.model.Content
import org.http4k.ai.mcp.model.Resource
import org.http4k.ai.mcp.model.apps.McpAppResourceMeta
import org.http4k.ai.mcp.model.apps.McpApps.MIME_TYPE
import org.http4k.connect.model.MimeType
import org.http4k.core.Uri
import org.http4k.template.TemplateRenderer
import org.http4k.template.ViewModel

/**
 * Renders a ViewModel into a ResourceResponse for use when rendering an MCP App.
 */
data class McpAppViewModelResourceHandler(
    val uiUri: Uri,
    private val renderer: TemplateRenderer,
    val meta: McpAppResourceMeta = McpAppResourceMeta(),
    val mimeType: MimeType = MIME_TYPE,
    private val fn: (ResourceRequest) -> ViewModel
) : ResourceHandler {
    override fun invoke(p1: ResourceRequest) =
        ResourceResponse.Ok(Resource.Content.Text(renderer(fn(p1)), uiUri, mimeType, Content.Meta(ui = meta)))
}
