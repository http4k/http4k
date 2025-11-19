package org.http4k.connect.model

import dev.forkhandles.values.IntValue
import dev.forkhandles.values.IntValueFactory

class TimeToLive private constructor(value: Int) : IntValue(value) {
    companion object : IntValueFactory<TimeToLive>(::TimeToLive)
}
