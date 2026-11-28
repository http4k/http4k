package org.http4k.ai.model

import dev.forkhandles.values.DoubleValue
import dev.forkhandles.values.DoubleValueFactory
import dev.forkhandles.values.between

class Temperature private constructor(value: Double) : DoubleValue(value) {
    companion object : DoubleValueFactory<Temperature>(::Temperature, (0.0..1.0).between) {
        val ZERO = of(0.0)
        val ONE = of(1.0)
    }
}
