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
    val instructions: String? = null,
) {
    constructor(
        entity: McpEntity,
        version: Version,
        vararg capabilities: ServerProtocolCapability,
        title: String? = null,
        instructions: String? = null,
        protocolVersions: Set<ProtocolVersion> = setOf(`2025-03-26`, `2024-11-05`),
    ) : this(VersionedMcpEntity(entity, version, title), protocolVersions, ServerCapabilities(*capabilities), instructions)

    constructor(
        entity: String,
        version: String,
        vararg capabilities: ServerProtocolCapability,
        title: String? = null,
        instructions: String? = null,
        protocolVersions: Set<ProtocolVersion> = setOf(`2025-03-26`, `2024-11-05`),
    ) : this(VersionedMcpEntity(entity, version, title), protocolVersions, ServerCapabilities(*capabilities), instructions)
}
