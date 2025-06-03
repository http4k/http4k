package org.http4k.ai.model

import dev.forkhandles.values.DoubleValue
import dev.forkhandles.values.DoubleValueFactory

class Temperature private constructor(value: Double) : DoubleValue(value) {
    companion object : DoubleValueFactory<Temperature>(::Temperature, _0_to_1) {
        val ZERO = Temperature.of(0.0)
        val ONE = Temperature.of(1.0)
    }
}
