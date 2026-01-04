package org.http4k.ai.mcp.protocol

import org.http4k.ai.mcp.model.Icon
import org.http4k.ai.mcp.model.McpEntity
import org.http4k.core.Uri
import se.ansman.kotshi.JsonSerializable

/**
 * A description of an entity taking part in the MCP protocol - can be a client or a server.
 */
@JsonSerializable
data class VersionedMcpEntity(
    val name: McpEntity,
    val version: Version,
    val title: String? = null,
    val description: String? = null,
    val websiteUrl: Uri? = null,
    val icons: List<Icon>? = null
) {
    constructor(
        name: String,
        version: String,
        title: String? = null,
        description: String? = null,
        websiteUrl: Uri? = null,
        icons: List<Icon>? = null
    ) : this(
        McpEntity.of(name),
        Version.of(version),
        title,
        description,
        websiteUrl,
        icons
    )
}

