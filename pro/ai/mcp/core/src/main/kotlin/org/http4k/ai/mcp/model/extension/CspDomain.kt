package org.http4k.ai.mcp.model.extension

import dev.forkhandles.values.NonBlankStringValueFactory
import dev.forkhandles.values.StringValue

class CspDomain private constructor(value: String) : StringValue(value) {
    companion object : NonBlankStringValueFactory<CspDomain>(::CspDomain)
}
