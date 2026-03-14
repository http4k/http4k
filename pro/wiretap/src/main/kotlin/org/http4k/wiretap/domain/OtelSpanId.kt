package org.http4k.wiretap.domain

import dev.forkhandles.values.NonBlankStringValueFactory
import dev.forkhandles.values.StringValue

class OtelSpanId private constructor(value: String) : StringValue(value) {
    companion object : NonBlankStringValueFactory<OtelSpanId>(::OtelSpanId)
}
