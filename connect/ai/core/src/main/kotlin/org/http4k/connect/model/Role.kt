package org.http4k.connect.model

import dev.forkhandles.values.NonBlankStringValueFactory
import dev.forkhandles.values.StringValue

class Role private constructor(value: String) : StringValue(value) {
    companion object : NonBlankStringValueFactory<Role>(::Role) {
        val System = Role.of("system")
        val User = Role.of("user")
        val Assistant = Role.of("assistant")
        val Tool = Role.of("tool")
    }
}
