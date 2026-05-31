package org.http4k.connect.amazon.core.model

import dev.forkhandles.values.Maskers.hidden
import dev.forkhandles.values.NonBlankStringValueFactory
import dev.forkhandles.values.StringValue

class SessionToken private constructor(value: String) : StringValue(value, hidden()) {
    companion object : NonBlankStringValueFactory<SessionToken>(::SessionToken)
}

