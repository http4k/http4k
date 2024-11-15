package org.http4k.connect.openai.model

import dev.forkhandles.values.NonBlankStringValueFactory
import dev.forkhandles.values.StringValue

class VerificationToken private constructor(value: String) : StringValue(value) {
    companion object : NonBlankStringValueFactory<VerificationToken>(::VerificationToken)
}
