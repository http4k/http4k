package org.http4k.connect.mcp.model

import dev.forkhandles.values.DoubleValue
import dev.forkhandles.values.DoubleValueFactory
import org.http4k.connect.mcp._0_to_1

class CostPriority private constructor(value: Double) : DoubleValue(value) {
    companion object : DoubleValueFactory<CostPriority>(::CostPriority, _0_to_1)
}
