/*
 * Copyright (c) 2025-present http4k Ltd. All rights reserved.
 * Licensed under the http4k Commercial License: https://http4k.org/commercial-license
 */
package org.http4k.routing.stateless

import org.http4k.ai.mcp.stateless.CompletionHandler
import org.http4k.ai.mcp.stateless.PromptHandler
import org.http4k.ai.mcp.stateless.ResourceHandler
import org.http4k.ai.mcp.stateless.ToolHandler
import org.http4k.ai.mcp.stateless.model.Prompt
import org.http4k.ai.mcp.stateless.model.Reference
import org.http4k.ai.mcp.stateless.model.Resource
import org.http4k.ai.mcp.stateless.model.Tool
import org.http4k.ai.mcp.stateless.protocol.ServerMetaData
import org.http4k.ai.mcp.stateless.server.capability.CompletionCapability
import org.http4k.ai.mcp.stateless.server.capability.PromptCapability
import org.http4k.ai.mcp.stateless.server.capability.ResourceCapability
import org.http4k.ai.mcp.stateless.server.capability.ServerCapability
import org.http4k.ai.mcp.stateless.server.capability.ToolCapability
import org.http4k.ai.mcp.stateless.server.http.HttpMcp
import org.http4k.ai.mcp.stateless.server.protocol.McpFilter
import org.http4k.ai.mcp.stateless.server.protocol.McpProtocol
import org.http4k.ai.mcp.stateless.server.protocol.McpServerExtension
import org.http4k.ai.mcp.stateless.server.protocol.NoOp
import org.http4k.ai.mcp.stateless.server.security.McpSecurity
import org.http4k.filter.CorsPolicy

/**
 * Create a stateless (2026-07-28) Streamable-HTTP MCP server from capability bindings.
 * POST /mcp -> single JSON response; GET/DELETE -> 405.
 * Security note: with the default corsPolicy = null there is no Origin protection.
 */
fun mcp(
    metadata: ServerMetaData,
    security: McpSecurity,
    vararg capabilities: ServerCapability,
    mcpFilter: McpFilter = McpFilter.NoOp,
    path: String = "/mcp",
    corsPolicy: CorsPolicy? = null,
    extensions: List<McpServerExtension> = emptyList()
) = HttpMcp(
    McpProtocol(metadata, *capabilities, mcpFilter = mcpFilter, extensions = extensions),
    security, path, corsPolicy
)

// The http face only — no subscriptions/listen SSE stream. For request/response contexts (e.g. serverless)
// where a long-lived stream can't be served.
fun mcpHttpNonStreaming(
    metadata: ServerMetaData,
    security: McpSecurity,
    vararg capabilities: ServerCapability,
    mcpFilter: McpFilter = McpFilter.NoOp,
    path: String = "/mcp",
    corsPolicy: CorsPolicy? = null,
    extensions: List<McpServerExtension> = emptyList()
) = requireNotNull(
    mcp(
        metadata, security, *capabilities,
        mcpFilter = mcpFilter, path = path, corsPolicy = corsPolicy, extensions = extensions
    ).http
)

infix fun Tool.bind(handler: ToolHandler) = ToolCapability(this, handler)
infix fun Prompt.bind(handler: PromptHandler) = PromptCapability(this, handler)
infix fun Resource.bind(handler: ResourceHandler) = ResourceCapability(this, handler)
infix fun Reference.bind(handler: CompletionHandler) = CompletionCapability(this, handler)
