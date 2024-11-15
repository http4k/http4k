package org.http4k.connect.amazon.sns.model

import dev.forkhandles.values.NonBlankStringValueFactory
import dev.forkhandles.values.StringValue

class PhoneNumber private constructor(value: String) : StringValue(value) {
    companion object : NonBlankStringValueFactory<PhoneNumber>(::PhoneNumber)
}
