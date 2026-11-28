package org.http4k.connect.amazon.instancemetadata.model

import dev.forkhandles.values.StringValue
import dev.forkhandles.values.StringValueFactory
import dev.forkhandles.values.toValidator

class InstanceId private constructor(value: String) : StringValue(value) {
    companion object : StringValueFactory<InstanceId>(::InstanceId, { it: String -> it.startsWith("i-") }.toValidator())
}
