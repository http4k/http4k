package org.http4k.connect.openfeature

import dev.forkhandles.values.NonBlankStringValueFactory
import dev.forkhandles.values.StringValue

class OpenFeatureToken private constructor(value: String) : StringValue(value) {
    companion object : NonBlankStringValueFactory<OpenFeatureToken>(::OpenFeatureToken)
}
