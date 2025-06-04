package org.http4k.ai.mcp.protocol

import org.http4k.ai.mcp.model.McpEntity
import org.http4k.ai.mcp.protocol.ProtocolVersion.Companion.`2024-11-05`
import org.http4k.ai.mcp.protocol.ProtocolVersion.Companion.`2025-03-26`
import se.ansman.kotshi.JsonSerializable

/**
 * Information about the server and it's capacities.
 */
@JsonSerializable
@ConsistentCopyVisibility
data class ServerMetaData internal constructor(
    val entity: VersionedMcpEntity,
    val protocolVersions: Set<ProtocolVersion> = setOf(`2025-03-26`, `2024-11-05`),
    val capabilities: ServerCapabilities = ServerCapabilities(),
) {
    constructor(
        entity: McpEntity,
        version: Version,
        vararg capabilities: ServerProtocolCapability,
        protocolVersions: Set<ProtocolVersion> = setOf(`2025-03-26`, `2024-11-05`),
    ) : this(VersionedMcpEntity(entity, version), protocolVersions, ServerCapabilities(*capabilities))

    constructor(
        entity: String,
        version: String,
        vararg capabilities: ServerProtocolCapability,
        protocolVersions: Set<ProtocolVersion> = setOf(`2025-03-26`, `2024-11-05`),
    ) : this(VersionedMcpEntity(entity, version), protocolVersions, ServerCapabilities(*capabilities))
}
