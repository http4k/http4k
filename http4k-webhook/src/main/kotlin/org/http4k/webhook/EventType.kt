package org.http4k.webhook

import dev.forkhandles.values.NonBlankStringValueFactory
import dev.forkhandles.values.StringValue

class EventType private constructor(value: String) : StringValue(value) {
    companion object : NonBlankStringValueFactory<EventType>(::EventType)
}
