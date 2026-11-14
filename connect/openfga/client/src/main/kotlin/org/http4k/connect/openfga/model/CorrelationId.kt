package org.http4k.connect.openfga.model

import dev.forkhandles.values.ComparableValue
import dev.forkhandles.values.NonBlankStringValueFactory
import dev.forkhandles.values.StringValue

class CorrelationId private constructor(value: String) : StringValue(value), ComparableValue<CorrelationId, String> {
    companion object : NonBlankStringValueFactory<CorrelationId>(::CorrelationId)
}
