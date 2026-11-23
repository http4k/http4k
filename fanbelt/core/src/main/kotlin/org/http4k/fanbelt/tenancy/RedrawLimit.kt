package org.http4k.fanbelt.tenancy

import dev.forkhandles.values.IntValue
import dev.forkhandles.values.IntValueFactory
import dev.forkhandles.values.minValue

class RedrawLimit private constructor(value: Int) : IntValue(value) {
    companion object : IntValueFactory<RedrawLimit>(::RedrawLimit, 1.minValue)
}
