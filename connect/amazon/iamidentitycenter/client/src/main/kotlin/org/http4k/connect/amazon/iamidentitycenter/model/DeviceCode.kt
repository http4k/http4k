package org.http4k.connect.amazon.iamidentitycenter.model

import dev.forkhandles.values.NonBlankStringValueFactory
import dev.forkhandles.values.StringValue

class DeviceCode private constructor(value: String) : StringValue(value) {
    companion object : NonBlankStringValueFactory<DeviceCode>(::DeviceCode)
}
