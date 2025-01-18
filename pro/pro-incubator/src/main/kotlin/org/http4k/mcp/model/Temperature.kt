package org.http4k.mcp.model

import dev.forkhandles.values.DoubleValue
import dev.forkhandles.values.DoubleValueFactory

class Temperature private constructor(value: Double) : DoubleValue(value) {
    companion object : DoubleValueFactory<Temperature>(::Temperature, _0_to_1)
}
