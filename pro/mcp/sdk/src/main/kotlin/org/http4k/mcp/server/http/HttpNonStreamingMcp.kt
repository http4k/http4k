package org.http4k.mcp.server.http

import org.http4k.core.then
import org.http4k.filter.ServerFilters.CatchAll
import org.http4k.filter.ServerFilters.CatchLensFailure
import org.http4k.mcp.server.protocol.McpProtocol
import org.http4k.sse.Sse

/**
 * Draft MCP server setup for Non-streaming HTTP-based MCP Servers
 *
 * NOTE THAT THIS IMPLEMENTATION IS BASED ON THE DRAFT MCP PROTOCOL AND IS SUBJECT TO CHANGE
 */
fun HttpNonStreamingMcp(mcpProtocol: McpProtocol<Sse>) =
    CatchAll().then(CatchLensFailure()).then(HttpNonStreamingMcpConnection(mcpProtocol))
