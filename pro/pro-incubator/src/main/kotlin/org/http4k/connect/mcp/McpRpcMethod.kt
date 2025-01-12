package org.http4k.connect.mcp

import dev.forkhandles.values.NonBlankStringValueFactory
import dev.forkhandles.values.StringValue

class McpRpcMethod private constructor(value: String) : StringValue(value) {
    companion object : NonBlankStringValueFactory<McpRpcMethod>(::McpRpcMethod)
}
