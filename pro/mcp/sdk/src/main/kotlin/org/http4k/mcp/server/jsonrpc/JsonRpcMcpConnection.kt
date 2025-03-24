package org.http4k.mcp.server.jsonrpc

import org.http4k.core.Request
import org.http4k.core.Response
import org.http4k.core.Status.Companion.BAD_REQUEST
import org.http4k.core.Status.Companion.NOT_FOUND
import org.http4k.mcp.protocol.ClientCapabilities.Companion.All
import org.http4k.mcp.protocol.VersionedMcpEntity
import org.http4k.mcp.protocol.messages.McpInitialize
import org.http4k.mcp.server.protocol.McpProtocol
import org.http4k.mcp.server.sessions.Session.Invalid
import org.http4k.mcp.server.sessions.Session.Valid
import org.http4k.routing.bind

/**
 * Routes inbound POST requests to the MCP server to the MCP protocol for processing and immediate response
 * via JSON RPC result messages.
 */
fun JsonRpcMcpConnection(protocol: McpProtocol<Unit, Response>) = "/jsonrpc" bind { req: Request ->
    when (val session = protocol.validate(req)) {
        is Valid -> {
            with(protocol) {
                handleInitialize(
                    McpInitialize.Request(VersionedMcpEntity(metaData.entity.name, metaData.entity.version), All),
                    session.sessionId
                )

                receive(Unit, session.sessionId, req)
            }
        }

        is Invalid -> Response(NOT_FOUND)
    }
}
