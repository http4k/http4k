package org.http4k.connect.kafka.rest.model

import dev.forkhandles.values.IntValue
import dev.forkhandles.values.IntValueFactory

class Offset private constructor(override val value: Int) : IntValue(value) {
    companion object : IntValueFactory<Offset>(::Offset)
}
