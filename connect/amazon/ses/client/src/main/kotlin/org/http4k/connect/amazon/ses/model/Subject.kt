package org.http4k.connect.amazon.ses.model

import dev.forkhandles.values.NonBlankStringValueFactory
import dev.forkhandles.values.StringValue

class Subject private constructor(value: String) : StringValue(value) {
    companion object : NonBlankStringValueFactory<Subject>(::Subject)
}
