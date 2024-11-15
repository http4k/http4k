package org.http4k.connect.amazon.ses.model

import dev.forkhandles.values.Maskers.obfuscated
import dev.forkhandles.values.NonBlankStringValueFactory
import dev.forkhandles.values.StringValue

class EmailAddress private constructor(value: String) : StringValue(value, obfuscated()) {
    companion object : NonBlankStringValueFactory<EmailAddress>(::EmailAddress)
}
