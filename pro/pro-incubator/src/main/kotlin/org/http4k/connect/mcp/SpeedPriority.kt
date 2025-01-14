package org.http4k.connect.mcp

import dev.forkhandles.values.DoubleValue
import dev.forkhandles.values.DoubleValueFactory

class SpeedPriority private constructor(value: Double) : DoubleValue(value) {
    companion object : DoubleValueFactory<SpeedPriority>(::SpeedPriority, _0_to_1)
}
