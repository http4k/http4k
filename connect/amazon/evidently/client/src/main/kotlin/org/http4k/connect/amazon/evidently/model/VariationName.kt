package org.http4k.connect.amazon.evidently.model

import dev.forkhandles.values.NonEmptyStringValueFactory
import dev.forkhandles.values.StringValue

class VariationName private constructor(value: String) : StringValue(value) {
    companion object : NonEmptyStringValueFactory<VariationName>(::VariationName)
}
