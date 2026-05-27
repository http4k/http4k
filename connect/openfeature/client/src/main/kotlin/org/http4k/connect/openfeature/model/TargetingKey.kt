package org.http4k.connect.openfeature.model

import dev.forkhandles.values.NonBlankStringValueFactory
import dev.forkhandles.values.StringValue

class TargetingKey private constructor(value: String) : StringValue(value) {
    companion object : NonBlankStringValueFactory<TargetingKey>(::TargetingKey)
}
