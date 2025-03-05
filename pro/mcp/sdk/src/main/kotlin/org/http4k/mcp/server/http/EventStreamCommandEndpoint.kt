package org.http4k.mcp.server.http

import org.http4k.core.Response
import org.http4k.mcp.model.McpEntity
import org.http4k.mcp.protocol.ClientCapabilities
import org.http4k.mcp.protocol.Version
import org.http4k.mcp.protocol.VersionedMcpEntity
import org.http4k.mcp.protocol.messages.McpInitialize
import org.http4k.mcp.server.protocol.McpProtocol
import org.http4k.routing.sse
import org.http4k.sse.Sse

/**
 * This SSE handler can be bound to whatever path is required by the server with
 * routes("/path" bind <HttpCommandHandler>
 */
fun EventStreamCommandEndpoint(mcpProtocol: McpProtocol<Response, Sse>) = sse {
    val newSession = mcpProtocol.newSession(it.connectRequest, it)

    mcpProtocol.handleInitialize(
        McpInitialize.Request(
            VersionedMcpEntity(McpEntity.of("server"), Version.of("1")),
            ClientCapabilities()
        ),
        newSession
    )

    mcpProtocol.receive(newSession, it.connectRequest)
}
