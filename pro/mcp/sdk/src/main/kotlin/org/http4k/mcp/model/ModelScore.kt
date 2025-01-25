package org.http4k.mcp.model

import dev.forkhandles.values.ComparableValue
import dev.forkhandles.values.DoubleValue
import dev.forkhandles.values.DoubleValueFactory

class ModelScore private constructor(value: Double) : DoubleValue(value), ComparableValue<ModelScore, Double> {
    companion object : DoubleValueFactory<ModelScore>(::ModelScore, _0_to_1) {
        val MAX = ModelScore.of(1.0)
    }
}
