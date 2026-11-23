package org.http4k.fanbelt.presence

import dev.forkhandles.values.NonBlankStringValueFactory
import dev.forkhandles.values.StringValue

class PresenceMember private constructor(value: String) : StringValue(value) {
    companion object : NonBlankStringValueFactory<PresenceMember>(::PresenceMember)
}
