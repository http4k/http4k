package org.http4k.mcp.server.jsonrpc

import org.http4k.core.Request
import org.http4k.core.Response
import org.http4k.core.Status.Companion.NOT_FOUND
import org.http4k.mcp.protocol.ClientCapabilities.Companion.All
import org.http4k.mcp.protocol.VersionedMcpEntity
import org.http4k.mcp.protocol.messages.McpInitialize
import org.http4k.mcp.server.protocol.InvalidSession
import org.http4k.mcp.server.protocol.McpProtocol
import org.http4k.mcp.server.protocol.Session
import org.http4k.mcp.util.asHttp
import org.http4k.routing.bind

/**
 * Routes inbound POST requests to the MCP server to the MCP protocol for processing and immediate response
 * via JSON RPC result messages.
 */
fun JsonRpcMcpConnection(protocol: McpProtocol<Unit>) = "/jsonrpc" bind { req: Request ->
    when (val session = protocol.retrieveSession(req)) {
        is Session -> {
            with(protocol) {
                handleInitialize(
                    McpInitialize.Request(VersionedMcpEntity(metaData.entity.name, metaData.entity.version), All),
                    session
                )
                receive(Unit, session, req).asHttp()
            }
        }

        is InvalidSession -> Response(NOT_FOUND)
    }
}
