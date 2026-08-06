package org.http4k.connect.amazon.iot.model

import dev.forkhandles.values.StringValue
import dev.forkhandles.values.StringValueFactory
import dev.forkhandles.values.and
import dev.forkhandles.values.maxLength
import dev.forkhandles.values.regex

/**
 * A stream identifier, unique within the account. AWS allows only alphanumerics, "-" and "_",
 * which is what lets it be dropped straight into the `/streams/{streamId}` path.
 */
class StreamId private constructor(value: String) : StringValue(value) {
    companion object : StringValueFactory<StreamId>(::StreamId, 128.maxLength.and("[a-zA-Z0-9_-]+".regex))
}
