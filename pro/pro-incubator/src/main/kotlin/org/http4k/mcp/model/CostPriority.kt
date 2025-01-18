package org.http4k.mcp.model

import dev.forkhandles.values.DoubleValue
import dev.forkhandles.values.DoubleValueFactory

class CostPriority private constructor(value: Double) : DoubleValue(value) {
    companion object : DoubleValueFactory<CostPriority>(::CostPriority, _0_to_1)
}
