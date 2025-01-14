package org.http4k.connect.mcp

import dev.forkhandles.values.DoubleValue
import dev.forkhandles.values.DoubleValueFactory

class IntelligencePriority private constructor(value: Double) : DoubleValue(value) {
    companion object : DoubleValueFactory<IntelligencePriority>(::IntelligencePriority, _0_to_1)
}
