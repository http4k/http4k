package org.http4k.connect.mcp

import dev.forkhandles.values.IntValue
import dev.forkhandles.values.IntValueFactory

class MaxTokens private constructor(value: Int) : IntValue(value) {
    companion object : IntValueFactory<MaxTokens>(::MaxTokens)
}
