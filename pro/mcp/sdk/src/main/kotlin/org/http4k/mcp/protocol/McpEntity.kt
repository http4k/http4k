package org.http4k.mcp.protocol

import dev.forkhandles.values.NonBlankStringValueFactory
import dev.forkhandles.values.StringValue

/**
 * Represents the name of the MCP entity. Used for identification and matching purposes.
 */
class McpEntity private constructor(value: String) : StringValue(value) {
    companion object : NonBlankStringValueFactory<McpEntity>(::McpEntity)
}
