package org.http4k.mcp.protocol

import org.http4k.mcp.model.McpEntity
import org.http4k.mcp.protocol.ProtocolVersion.Companion.LATEST_VERSION
import se.ansman.kotshi.JsonSerializable

/**
 * Information about the server and it's capacities.
 */
@JsonSerializable
@ConsistentCopyVisibility
data class ServerMetaData internal constructor(
    val entity: VersionedMcpEntity,
    val protocolVersion: ProtocolVersion = LATEST_VERSION,
    val capabilities: ServerCapabilities = ServerCapabilities(),
) {
    constructor(
        entity: McpEntity,
        version: Version,
        vararg capabilities: ProtocolCapability,
        protocolVersion: ProtocolVersion = LATEST_VERSION
    ) : this(VersionedMcpEntity(entity, version), protocolVersion, ServerCapabilities(*capabilities))
}
