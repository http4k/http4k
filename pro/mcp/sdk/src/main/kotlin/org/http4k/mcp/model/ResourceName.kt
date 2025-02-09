package org.http4k.mcp.model

import dev.forkhandles.values.NonBlankStringValueFactory
import dev.forkhandles.values.StringValue

class ResourceName private constructor(value: String) : StringValue(value) {
    companion object : NonBlankStringValueFactory<ResourceName>(::ResourceName)
}
