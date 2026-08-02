package org.http4k.connect.amazon.iotdataplane.model

import dev.forkhandles.values.StringValue
import dev.forkhandles.values.StringValueFactory
import dev.forkhandles.values.regex

/**
 * The identifier of the MQTT client whose connection is being addressed. AWS caps it at 128
 * characters and reserves the `$` prefix.
 */
class ClientId private constructor(value: String) : StringValue(value) {
    companion object : StringValueFactory<ClientId>(::ClientId, "[^\$].{0,127}".regex)
}
