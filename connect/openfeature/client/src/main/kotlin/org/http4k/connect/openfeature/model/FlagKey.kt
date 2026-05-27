package org.http4k.connect.openfeature.model

import dev.forkhandles.values.NonBlankStringValueFactory
import dev.forkhandles.values.StringValue

class FlagKey private constructor(value: String) : StringValue(value) {
    companion object : NonBlankStringValueFactory<FlagKey>(::FlagKey)
}
