package org.http4k.connect.amazon.core.model

import dev.forkhandles.values.StringValue
import dev.forkhandles.values.StringValueFactory
import dev.forkhandles.values.and
import dev.forkhandles.values.maxLength
import dev.forkhandles.values.minLength

class VpcId private constructor(value: String): StringValue(value) {
    companion object : StringValueFactory<VpcId>(::VpcId, 1.minLength.and(1024.maxLength))
}
