/*
 * Copyright (c) 2025-present http4k Ltd. All rights reserved.
 * Licensed under the http4k Commercial License: https://http4k.org/commercial-license
 */
package org.http4k.routing

import dev.forkhandles.time.executors.SimpleScheduler
import dev.forkhandles.time.executors.SimpleSchedulerService
import org.http4k.ai.mcp.CompletionHandler
import org.http4k.ai.mcp.PromptHandler
import org.http4k.ai.mcp.ResourceHandler
import org.http4k.ai.mcp.ToolHandler
import org.http4k.ai.mcp.model.Prompt
import org.http4k.ai.mcp.model.Reference
import org.http4k.ai.mcp.model.Resource
import org.http4k.ai.mcp.model.Tool
import org.http4k.ai.mcp.protocol.ServerMetaData
import org.http4k.ai.mcp.protocol.SessionId
import org.http4k.ai.mcp.server.capability.CompletionCapability
import org.http4k.ai.mcp.server.capability.PromptCapability
import org.http4k.ai.mcp.server.capability.ResourceCapability
import org.http4k.ai.mcp.server.capability.ServerCapability
import org.http4k.ai.mcp.server.capability.initializer
import org.http4k.ai.mcp.server.capability.SimpleInitializeHandler
import org.http4k.ai.mcp.server.capability.ToolCapability
import org.http4k.ai.mcp.server.capability.completions
import org.http4k.ai.mcp.server.capability.prompts
import org.http4k.ai.mcp.server.capability.resources
import org.http4k.ai.mcp.server.capability.tools
import org.http4k.ai.mcp.server.http.HttpNonStreamingMcp
import org.http4k.ai.mcp.server.http.HttpSessions
import org.http4k.ai.mcp.server.http.HttpStreamingMcp
import org.http4k.ai.mcp.server.jsonrpc.JsonRpcMcp
import org.http4k.ai.mcp.server.jsonrpc.JsonRpcSessions
import org.http4k.ai.mcp.server.protocol.McpFilter
import org.http4k.ai.mcp.server.protocol.McpProtocol
import org.http4k.ai.mcp.server.protocol.NoOp
import org.http4k.ai.mcp.server.protocol.Session
import org.http4k.ai.mcp.server.security.McpSecurity
import org.http4k.ai.mcp.server.sse.SseMcp
import org.http4k.ai.mcp.server.sse.SseSessions
import org.http4k.ai.mcp.server.stdio.StdIoMcpSessions
import org.http4k.ai.mcp.server.websocket.WebsocketMcp
import org.http4k.ai.mcp.server.websocket.WebsocketSessions
import org.http4k.core.Method.POST
import org.http4k.core.Request
import java.io.Reader
import java.io.Writer
import java.time.Duration.ZERO
import java.util.UUID

/**
 * Create an HTTP (+ SSE) MCP app from a set of capability bindings.
 *
 *  The standard paths used are:
 *      /mcp (accept EventStream) <-- setup streaming connection to an MCP client
 *      /mcp (POST) <-- receive non-streaming messages from connected MCP clients
 *      /mcp (DELETE) <-- delete a session
 */
fun mcp(
    metadata: ServerMetaData,
    security: McpSecurity,
    vararg capabilities: ServerCapability,
    mcpFilter: McpFilter = McpFilter.NoOp,
    path: String = "/mcp"
) = HttpStreamingMcp(
    McpProtocol(
        metadata, HttpSessions().apply { start() },
        mcpFilter,
        capabilities = capabilities
    ),
    security,
    path
)

@Deprecated("Renamed to mcp()")
fun mcpHttpStreaming(
    metadata: ServerMetaData,
    security: McpSecurity,
    vararg capabilities: ServerCapability,
    mcpFilter: McpFilter = McpFilter.NoOp,
    path: String = "/mcp"
) = mcp(metadata, security, *capabilities, mcpFilter = mcpFilter, path = path)


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
    path: String = "/mcp"
) =
    HttpNonStreamingMcp(
        McpProtocol(metadata, HttpSessions().apply { start() }, mcpFilter, *capabilities),
        security,
        path
    )

/**
 * Create an SSE MCP app from a set of capability bindings.
 *
 * This is the main entry point for the MCP server. It sets up the SSE connection and then provides a
 *  endpoint for the client to send messages to.
 *
 *  The standard paths used are:
 *      /sse <-- setup the SSE connection to an MCP client
 *      /messages (POST) <-- receive messages from connected MCP clients
 */
fun mcpSse(
    metadata: ServerMetaData,
    security: McpSecurity,
    vararg capabilities: ServerCapability,
    mcpFilter: McpFilter = McpFilter.NoOp
) =
    SseMcp(McpProtocol(metadata, SseSessions().apply { start() }, mcpFilter, *capabilities), security)

/**
 * Create an HTTP MCP app from a set of capability bindings.
 *
 *  The standard paths used are:
 *      /ws <-- setup the WS connection to an MCP client
 */
fun mcpWebsocket(
    metadata: ServerMetaData,
    security: McpSecurity,
    vararg capabilities: ServerCapability,
    mcpFilter: McpFilter = McpFilter.NoOp
) =
    WebsocketMcp(McpProtocol(metadata, WebsocketSessions().apply { start() }, mcpFilter, *capabilities), security)

/**
 * Create an HTTP (pure JSONRPC) MCP app from a set of capability bindings.
 *
 *  The standard paths used are:
 *      /jsonrpc (POST) <-- receive messages from connected MCP clients
 */
fun mcpJsonRpc(
    metadata: ServerMetaData,
    security: McpSecurity,
    vararg capabilities: ServerCapability,
    mcpFilter: McpFilter = McpFilter.NoOp,
) =
    JsonRpcMcp(McpProtocol(metadata, JsonRpcSessions(), mcpFilter, *capabilities), security)

/**
 * Create a StdIO MCP app from a set of capability bindings.
 */
fun mcpStdIo(
    metadata: ServerMetaData,
    vararg capabilities: ServerCapability,
    reader: Reader = System.`in`.reader(),
    writer: Writer = System.out.writer(),
    executor: SimpleScheduler = SimpleSchedulerService(1),
    mcpFilter: McpFilter = McpFilter.NoOp,
) = McpProtocol(
    StdIoMcpSessions(writer),
    initializer(SimpleInitializeHandler(metadata)),
    tools(capabilities.filterIsInstance<ToolCapability>()),
    resources(capabilities.filterIsInstance<ResourceCapability>()),
    prompts(capabilities.filterIsInstance<PromptCapability>()),
    completions(capabilities.filterIsInstance<CompletionCapability>()),
    mcpFilter = mcpFilter,
).apply {
    executor.schedule({
        reader.buffered().lineSequence().forEach { it: String ->
            try {
                receive(Unit, Session(SessionId.of(UUID(0, 0).toString())), Request(POST, "").body(it))
            } catch (e: Exception) {
                e.printStackTrace(System.err)
            }
        }
    }, ZERO)
}

infix fun Tool.bind(handler: ToolHandler) = ToolCapability(this, handler)
infix fun Prompt.bind(handler: PromptHandler) = PromptCapability(this, handler)
infix fun Resource.bind(handler: ResourceHandler) = ResourceCapability(this, handler)
infix fun Reference.bind(handler: CompletionHandler) = CompletionCapability(this, handler)
