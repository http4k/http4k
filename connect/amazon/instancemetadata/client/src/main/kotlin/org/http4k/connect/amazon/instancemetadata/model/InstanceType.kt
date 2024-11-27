package org.http4k.connect.amazon.instancemetadata.model

import dev.forkhandles.values.NonEmptyStringValueFactory
import dev.forkhandles.values.StringValue

class InstanceType private constructor(value: String) : StringValue(value) {
    companion object : NonEmptyStringValueFactory<InstanceType>(::InstanceType)
}
