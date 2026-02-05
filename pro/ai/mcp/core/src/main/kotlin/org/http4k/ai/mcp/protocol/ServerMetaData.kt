package org.http4k.ai.mcp.protocol

import org.http4k.ai.mcp.model.McpEntity
import org.http4k.ai.mcp.protocol.ProtocolVersion.Companion.PUBLISHED
import se.ansman.kotshi.JsonSerializable

/**
 * Information about the server and it's capacities.
 */
@JsonSerializable
@ConsistentCopyVisibility
data class ServerMetaData internal constructor(
    val entity: VersionedMcpEntity,
    val protocolVersions: Set<ProtocolVersion> = PUBLISHED,
    val capabilities: ServerCapabilities = ServerCapabilities(),
    val instructions: String? = null,
) {
    constructor(
        entity: McpEntity,
        version: Version,
        vararg capabilities: ServerProtocolCapability,
        title: String? = null,
        instructions: String? = null,
        protocolVersions: Set<ProtocolVersion> = PUBLISHED,
    ) : this(VersionedMcpEntity(entity, version, title), protocolVersions, ServerCapabilities(*capabilities), instructions)

    constructor(
        entity: String,
        version: String,
        vararg capabilities: ServerProtocolCapability,
        title: String? = null,
        instructions: String? = null,
        protocolVersions: Set<ProtocolVersion> = PUBLISHED,
    ) : this(VersionedMcpEntity(entity, version, title), protocolVersions, ServerCapabilities(*capabilities), instructions)

    constructor(
        entity: McpEntity,
        version: Version,
        capabilities: ServerCapabilities,
        title: String? = null,
        instructions: String? = null,
        protocolVersions: Set<ProtocolVersion> = PUBLISHED,
    ) : this(VersionedMcpEntity(entity, version, title), protocolVersions, capabilities, instructions)
}
