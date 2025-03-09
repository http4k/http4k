package org.http4k.mcp.protocol

import org.http4k.mcp.model.McpEntity
import se.ansman.kotshi.JsonSerializable

/**
 * A description of an entity taking part in the MCP protocol - can be a client or a server.
 */
@JsonSerializable
data class VersionedMcpEntity(val name: McpEntity, val version: Version) {
    constructor(name: String, version: String) : this(McpEntity.of(name), Version.of(version))
}

