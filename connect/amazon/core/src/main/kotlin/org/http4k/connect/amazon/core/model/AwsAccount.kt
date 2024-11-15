package org.http4k.connect.amazon.core.model

import dev.forkhandles.values.StringValue
import dev.forkhandles.values.StringValueFactory
import dev.forkhandles.values.and
import dev.forkhandles.values.minLength

class AwsAccount private constructor(value: String) : StringValue(value.padStart(12, '0')) {
    companion object : StringValueFactory<AwsAccount>(::AwsAccount, 1.minLength.and { it.all(Char::isDigit) })
}
