/*
 * Copyright (c) 2025-present http4k Ltd. All rights reserved.
 * Licensed under the http4k Commercial License: https://http4k.org/commercial-license
 */
package org.http4k.routing

import org.http4k.ai.mcp.CompletionHandler
import org.http4k.ai.mcp.PromptHandler
import org.http4k.ai.mcp.ResourceHandler
import org.http4k.ai.mcp.ToolHandler
import org.http4k.ai.mcp.model.Prompt
import org.http4k.ai.mcp.model.Reference
import org.http4k.ai.mcp.model.Resource
import org.http4k.ai.mcp.model.Tool
import org.http4k.ai.mcp.protocol.ServerMetaData
import org.http4k.ai.mcp.server.capability.CompletionCapability
import org.http4k.ai.mcp.server.capability.PromptCapability
import org.http4k.ai.mcp.server.capability.ResourceCapability
import org.http4k.ai.mcp.server.capability.ServerCapability
import org.http4k.ai.mcp.server.capability.ToolCapability
import org.http4k.ai.mcp.server.http.HttpNonStreamingMcp
import org.http4k.ai.mcp.server.http.HttpSessions
import org.http4k.ai.mcp.server.http.HttpStreamingMcp
import org.http4k.ai.mcp.server.protocol.McpFilter
import org.http4k.ai.mcp.server.protocol.McpProtocol
import org.http4k.ai.mcp.server.protocol.NoOp
import org.http4k.ai.mcp.server.security.McpSecurity
import org.http4k.filter.CorsPolicy

/**
 * Create an HTTP (+ SSE) MCP server from a set of capability bindings.
 *
 *  The standard paths used are:
 *      /mcp (accept EventStream) <-- setup streaming connection to an MCP client
 *      /mcp (POST) <-- receive non-streaming messages from connected MCP clients
 *      /mcp (DELETE) <-- delete a session
 *
 * Security note: with the default corsPolicy = null there is no Origin protection.
 */
fun mcp(
    metadata: ServerMetaData,
    security: McpSecurity,
    vararg capabilities: ServerCapability,
    mcpFilter: McpFilter = McpFilter.NoOp,
    path: String = "/mcp",
    corsPolicy: CorsPolicy? = null
) = HttpStreamingMcp(
    McpProtocol(
        metadata, HttpSessions().apply { start() },
        mcpFilter,
        capabilities = capabilities
    ),
    security,
    path,
    corsPolicy
)

/**
 * Create an HTTP (non-streaming) MCP app from a set of capability bindings.
 *
 *  The standard paths used are:
 *      /mcp (POST) <-- receive non-streaming messages from connected MCP clients
 */
fun mcpHttpNonStreaming(
    metadata: ServerMetaData,
    security: McpSecurity,
    vararg capabilities: ServerCapability,
    mcpFilter: McpFilter = McpFilter.NoOp,
    path: String = "/mcp",
    corsPolicy: CorsPolicy? = null
) =
    HttpNonStreamingMcp(
        McpProtocol(metadata, HttpSessions().apply { start() }, mcpFilter, *capabilities),
        security,
        path,
        corsPolicy
    )

infix fun Tool.bind(handler: ToolHandler) = ToolCapability(this, handler)
infix fun Prompt.bind(handler: PromptHandler) = PromptCapability(this, handler)
infix fun Resource.bind(handler: ResourceHandler) = ResourceCapability(this, handler)
infix fun Reference.bind(handler: CompletionHandler) = CompletionCapability(this, handler)
