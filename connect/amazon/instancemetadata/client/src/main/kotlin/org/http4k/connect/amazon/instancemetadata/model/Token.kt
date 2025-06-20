package org.http4k.connect.amazon.instancemetadata.model

import dev.forkhandles.values.NonEmptyStringValueFactory
import dev.forkhandles.values.StringValue

class Token private constructor(value: String): StringValue(value) {
    companion object: NonEmptyStringValueFactory<Token>(::Token)
}
