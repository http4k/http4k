package org.http4k.connect.openai.model

import dev.forkhandles.values.NonBlankStringValueFactory
import dev.forkhandles.values.StringValue

class AuthedSystem private constructor(value: String) : StringValue(value) {
    companion object : NonBlankStringValueFactory<AuthedSystem>(::AuthedSystem) {
        val openai = AuthedSystem.of("openai")
    }
}
