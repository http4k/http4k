package org.http4k.connect.amazon.cloudwatch.model

import dev.forkhandles.values.NonBlankStringValueFactory
import org.http4k.connect.amazon.core.model.ResourceId

class AlarmName private constructor(value: String) : ResourceId(value) {
    companion object : NonBlankStringValueFactory<AlarmName>(::AlarmName)
}
