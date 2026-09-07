package org.http4k.connect.amazon.xray.model

import dev.forkhandles.values.StringValue
import dev.forkhandles.values.StringValueFactory
import dev.forkhandles.values.regex

/** Version 1, the epoch second the trace started in hex, and 96 bits of hex randomness. */
class TraceId private constructor(value: String) : StringValue(value) {
    companion object : StringValueFactory<TraceId>(::TraceId, "1-[0-9a-f]{8}-[0-9a-f]{24}".regex)
}
