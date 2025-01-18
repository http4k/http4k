package org.http4k.mcp.model

import dev.forkhandles.values.DoubleValue
import dev.forkhandles.values.DoubleValueFactory
import org.http4k.connect.mcp._0_to_1

class Temperature private constructor(value: Double) : DoubleValue(value) {
    companion object : DoubleValueFactory<Temperature>(::Temperature, _0_to_1)
}
