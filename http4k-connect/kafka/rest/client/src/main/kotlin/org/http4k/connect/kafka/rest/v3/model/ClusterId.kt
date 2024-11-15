package org.http4k.connect.kafka.rest.v3.model

import dev.forkhandles.values.NonBlankStringValueFactory
import dev.forkhandles.values.StringValue

class ClusterId private constructor(value: String) : StringValue(value) {
    companion object : NonBlankStringValueFactory<ClusterId>(::ClusterId)
}
