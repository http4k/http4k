package org.http4k.mcp.protocol

import dev.forkhandles.values.NonBlankStringValueFactory
import dev.forkhandles.values.StringValue
import org.http4k.lens.Header
import org.http4k.lens.value

class Version private constructor(value: String) : StringValue(value) {
    companion object : NonBlankStringValueFactory<Version>(::Version)
}

val MCP_PROTOCOL_VERSION = Header.value(ProtocolVersion).optional("MCP-Protocol-Version")
