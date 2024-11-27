package org.http4k.connect.amazon.cloudwatchlogs.model

import dev.forkhandles.values.IntValue
import dev.forkhandles.values.IntValueFactory

class LogIndex private constructor(value: Int) : IntValue(value) {
    companion object : IntValueFactory<LogIndex>(::LogIndex)
}
