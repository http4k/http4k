package org.http4k.mcp.server

import org.http4k.mcp.protocol.McpEntity
import org.http4k.mcp.protocol.ProtocolVersion
import org.http4k.mcp.protocol.ProtocolVersion.Companion.LATEST_VERSION
import org.http4k.mcp.protocol.ServerCapabilities

/**
 * Information about the server and it's capacities.
 */
data class ServerMetaData(
    val entity: McpEntity,
    val protocolVersion: ProtocolVersion = LATEST_VERSION,
    val capabilities: ServerCapabilities = ServerCapabilities(),
)
