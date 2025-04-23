package org.http4k.a2a.protocol.model

import dev.forkhandles.values.NonBlankStringValueFactory
import dev.forkhandles.values.StringValue

class TaskId private constructor(value: String) : StringValue(value) {
    companion object : NonBlankStringValueFactory<TaskId>(::TaskId)
}
