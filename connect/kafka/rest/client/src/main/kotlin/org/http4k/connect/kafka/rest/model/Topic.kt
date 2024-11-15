package org.http4k.connect.kafka.rest.model

import dev.forkhandles.values.NonBlankStringValueFactory
import dev.forkhandles.values.StringValue

class Topic private constructor(override val value: String) : StringValue(value) {
    companion object : NonBlankStringValueFactory<Topic>(::Topic)
}

