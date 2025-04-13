package org.http4k.mcp.model

import dev.forkhandles.values.IntValue
import dev.forkhandles.values.IntValueFactory

class Size private constructor(value: Int) : IntValue(value) {
    companion object : IntValueFactory<Size>(::Size)
}
