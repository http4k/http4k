package org.http4k.connect.amazon.xray.model

import dev.forkhandles.values.StringValue
import dev.forkhandles.values.StringValueFactory
import dev.forkhandles.values.regex

class SegmentId private constructor(value: String) : StringValue(value) {
    companion object : StringValueFactory<SegmentId>(::SegmentId, "[0-9a-f]{16}".regex)
}
