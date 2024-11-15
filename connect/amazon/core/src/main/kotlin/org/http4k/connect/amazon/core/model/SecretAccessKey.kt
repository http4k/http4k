package org.http4k.connect.amazon.core.model

import dev.forkhandles.values.NonBlankStringValueFactory
import dev.forkhandles.values.StringValue

class SecretAccessKey private constructor(value: String) : StringValue(value) {
    companion object : NonBlankStringValueFactory<SecretAccessKey>(::SecretAccessKey)
}
