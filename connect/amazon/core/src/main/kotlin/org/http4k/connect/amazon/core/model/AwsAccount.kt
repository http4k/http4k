package org.http4k.connect.amazon.core.model

import dev.forkhandles.values.StringValue
import dev.forkhandles.values.StringValueFactory
import dev.forkhandles.values.regex

class AwsAccount private constructor(value: String) : StringValue(value.padStart(12, '0')) {
    companion object : StringValueFactory<AwsAccount>(::AwsAccount, "[0-9]+".regex)
}
