package org.http4k.connect.kafka.schemaregistry.model

import dev.forkhandles.values.IntValue
import dev.forkhandles.values.IntValueFactory

class Version private constructor(override val value: Int) : IntValue(value) {
    companion object : IntValueFactory<Version>(::Version)
}
