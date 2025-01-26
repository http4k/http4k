package org.http4k.mcp.protocol

import org.http4k.mcp.protocol.ProtocolVersion.Companion.LATEST_VERSION
import se.ansman.kotshi.JsonSerializable

/**
 * Information about the server and it's capacities.
 */
@JsonSerializable
data class ServerMetaData internal constructor(
    val entity: VersionedMcpEntity,
    val protocolVersion: ProtocolVersion = LATEST_VERSION,
    val capabilities: ServerCapabilities = ServerCapabilities(),
) {
    constructor(
        entity: McpEntity,
        version: Version,
        protocolVersion: ProtocolVersion = LATEST_VERSION,
        capabilities: ServerCapabilities = ServerCapabilities()
    ) : this(VersionedMcpEntity(entity, version), protocolVersion, capabilities)
}
