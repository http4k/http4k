package org.http4k.ai.mcp.server.jsonrpc

import org.http4k.ai.mcp.protocol.ClientCapabilities.Companion.All
import org.http4k.ai.mcp.protocol.messages.McpInitialize
import org.http4k.ai.mcp.server.protocol.InvalidSession
import org.http4k.ai.mcp.server.protocol.McpProtocol
import org.http4k.ai.mcp.server.protocol.Session
import org.http4k.ai.mcp.util.asHttp
import org.http4k.core.Request
import org.http4k.core.Response
import org.http4k.core.Status.Companion.NOT_FOUND
import org.http4k.core.Status.Companion.OK
import org.http4k.core.with
import org.http4k.lens.Header
import org.http4k.lens.MCP_SESSION_ID
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
                    McpInitialize.Request(metaData.entity, All),
                    session
                )
                receive(Unit, session, req).asHttp(OK)
                    .with(Header.MCP_SESSION_ID of session.id)
            }
        }

        is InvalidSession -> Response(NOT_FOUND)
    }
}
