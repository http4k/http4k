package org.http4k.connect.amazon.sqs.model

import dev.forkhandles.values.NonBlankStringValueFactory
import dev.forkhandles.values.StringValue

class SQSMessageId private constructor(value: String) : StringValue(value) {
    companion object : NonBlankStringValueFactory<SQSMessageId>(::SQSMessageId)
}
