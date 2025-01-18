package org.http4k.connect.mcp.protocol

import dev.forkhandles.values.NonBlankStringValueFactory
import dev.forkhandles.values.StringValue

class Version private constructor(value: String) : StringValue(value) {
    companion object : NonBlankStringValueFactory<Version>(::Version)
}
