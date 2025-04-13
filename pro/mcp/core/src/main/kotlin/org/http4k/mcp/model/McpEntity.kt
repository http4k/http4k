package org.http4k.mcp.model

import dev.forkhandles.values.NonBlankStringValueFactory
import dev.forkhandles.values.StringValue

/**
 * Represents the name of the MCP entity. Used for identification and matching purposes.
 */
class McpEntity private constructor(value: String) : StringValue(value), CapabilitySpec {
    companion object : NonBlankStringValueFactory<McpEntity>(::McpEntity)
}
