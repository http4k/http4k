package org.http4k.connect.amazon.scheduler.model

import dev.forkhandles.values.NonBlankStringValueFactory
import dev.forkhandles.values.StringValue

class ScheduleExpression private constructor(value: String) : StringValue(value) {
    companion object : NonBlankStringValueFactory<ScheduleExpression>(::ScheduleExpression)
}
