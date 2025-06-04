package org.http4k.ai.mcp.server.jsonrpc

import org.http4k.core.HttpFilter
import org.http4k.core.then
import org.http4k.filter.ServerFilters.CatchAll
import org.http4k.filter.ServerFilters.CatchLensFailure
import org.http4k.ai.mcp.server.protocol.McpProtocol
import org.http4k.ai.mcp.server.security.McpSecurity
import org.http4k.routing.routes

/**
 * Non-standard (but compliant) MCP server setup for JSONRPC-based (non streaming) MCP Servers
 */
fun JsonRpcMcp(protocol: McpProtocol<Unit>, security: McpSecurity) =
    CatchAll().then(CatchLensFailure()).then(
        routes(security.routes + HttpFilter(security).then(JsonRpcMcpConnection(protocol)))
    )
