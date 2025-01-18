package org.http4k.mcp.server

import org.http4k.mcp.protocol.Version

/**
 * A description of an entity taking part in the MCP protocol - can be a client or a server.
 */
data class McpEntity(val name: String, val version: Version)

