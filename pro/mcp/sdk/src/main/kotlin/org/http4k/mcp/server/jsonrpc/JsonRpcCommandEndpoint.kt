package org.http4k.mcp.server.jsonrpc

import org.http4k.core.Request
import org.http4k.core.Response
import org.http4k.mcp.protocol.ClientCapabilities
import org.http4k.mcp.protocol.VersionedMcpEntity
import org.http4k.mcp.protocol.messages.McpInitialize
import org.http4k.mcp.server.protocol.McpProtocol

/**
 * This SSE handler can be bound to whatever path is required by the server with
 * routes("/path" bind <HttpCommandHandler>
 */
fun JsonRpcCommandEndpoint(protocol: McpProtocol<Unit, Response>) = { it: Request ->
    val newSession = protocol.newSession(it, Unit)

    protocol.handleInitialize(
        McpInitialize.Request(
            VersionedMcpEntity(protocol.metaData.entity.name, protocol.metaData.entity.version),
            ClientCapabilities()
        ),
        newSession
    )

    protocol.receive(newSession, it)
}
