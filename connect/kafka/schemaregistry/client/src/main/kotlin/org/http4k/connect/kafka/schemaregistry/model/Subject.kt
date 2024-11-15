package org.http4k.connect.kafka.schemaregistry.model

import dev.forkhandles.values.NonBlankStringValueFactory
import dev.forkhandles.values.StringValue

class Subject private constructor(override val value: String) : StringValue(value) {
    companion object : NonBlankStringValueFactory<Subject>(::Subject)
}
