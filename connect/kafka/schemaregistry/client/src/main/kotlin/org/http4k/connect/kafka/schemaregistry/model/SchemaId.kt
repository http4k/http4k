package org.http4k.connect.kafka.schemaregistry.model

import dev.forkhandles.values.IntValue
import dev.forkhandles.values.IntValueFactory
import dev.forkhandles.values.minValue

class SchemaId private constructor(value: Int) : IntValue(value) {
    companion object : IntValueFactory<SchemaId>(::SchemaId, 0.minValue)
}
