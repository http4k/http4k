package org.http4k.connect.amazon.iotjobsdataplane.model

import dev.forkhandles.values.StringValue
import dev.forkhandles.values.StringValueFactory
import dev.forkhandles.values.and
import dev.forkhandles.values.maxLength
import dev.forkhandles.values.regex

/**
 * AWS does not allow slashes in a thing name, which is what lets it be dropped straight into the
 * `/things/{thingName}/jobs` path.
 */
class ThingName private constructor(value: String) : StringValue(value) {
    companion object : StringValueFactory<ThingName>(::ThingName, 128.maxLength.and("[a-zA-Z0-9:_-]+".regex))
}
