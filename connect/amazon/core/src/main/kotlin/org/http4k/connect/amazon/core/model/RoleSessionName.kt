package org.http4k.connect.amazon.core.model

import dev.forkhandles.values.NonBlankStringValueFactory
import dev.forkhandles.values.StringValue

class RoleSessionName private constructor(value: String) : StringValue(value) {
    companion object : NonBlankStringValueFactory<RoleSessionName>(::RoleSessionName)
}
