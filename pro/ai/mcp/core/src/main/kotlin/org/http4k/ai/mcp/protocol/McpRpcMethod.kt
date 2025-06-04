package org.http4k.ai.mcp.protocol

import dev.forkhandles.values.NonBlankStringValueFactory
import dev.forkhandles.values.StringValue

class McpRpcMethod private constructor(value: String) : StringValue(value) {
    companion object : NonBlankStringValueFactory<McpRpcMethod>(::McpRpcMethod)
}
