package org.http4k.connect.amazon.sns.model

import dev.forkhandles.values.NonBlankStringValueFactory
import dev.forkhandles.values.StringValue

class SNSMessageId private constructor(value: String) : StringValue(value) {
    companion object : NonBlankStringValueFactory<SNSMessageId>(::SNSMessageId)
}
