package org.http4k.ai.mcp.protocol

import org.http4k.ai.mcp.model.McpEntity
import se.ansman.kotshi.JsonSerializable

/**
 * A description of an entity taking part in the MCP protocol - can be a client or a server.
 */
@JsonSerializable
data class VersionedMcpEntity(val name: McpEntity, val version: Version, val title: String? = null) {
    constructor(name: String, version: String, title: String? = null) : this(
        McpEntity.of(name),
        Version.of(version),
        title
    )
}

