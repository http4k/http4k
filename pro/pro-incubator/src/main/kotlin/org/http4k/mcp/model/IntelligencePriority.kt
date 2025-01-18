package org.http4k.mcp.model

import dev.forkhandles.values.DoubleValue
import dev.forkhandles.values.DoubleValueFactory
import org.http4k.connect.mcp._0_to_1

class IntelligencePriority private constructor(value: Double) : DoubleValue(value) {
    companion object : DoubleValueFactory<IntelligencePriority>(::IntelligencePriority, _0_to_1)
}
