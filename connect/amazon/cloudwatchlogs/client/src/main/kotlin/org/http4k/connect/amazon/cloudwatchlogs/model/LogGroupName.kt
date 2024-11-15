package org.http4k.connect.amazon.cloudwatchlogs.model

import dev.forkhandles.values.NonBlankStringValueFactory
import org.http4k.connect.amazon.core.model.ResourceId

class LogGroupName private constructor(value: String) : ResourceId(value) {
    companion object : NonBlankStringValueFactory<LogGroupName>(::LogGroupName)
}
