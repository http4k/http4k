package org.http4k.connect.mcp

import dev.forkhandles.values.IntValue
import dev.forkhandles.values.IntValueFactory
import dev.forkhandles.values.and
import dev.forkhandles.values.maxValue
import dev.forkhandles.values.minValue

class MaxTokens private constructor(value: Int) : IntValue(value) {
    companion object : IntValueFactory<MaxTokens>(::MaxTokens, 0.minValue.and(1.maxValue))
}
