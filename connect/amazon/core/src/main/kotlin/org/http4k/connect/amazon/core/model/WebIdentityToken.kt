package org.http4k.connect.amazon.core.model

import dev.forkhandles.values.NonEmptyStringValueFactory
import dev.forkhandles.values.StringValue
import java.io.File

class WebIdentityToken private constructor(value: String) : StringValue(value) {
    companion object : NonEmptyStringValueFactory<WebIdentityToken>(::WebIdentityToken) {
        fun of(file: File) = of(file.readText())
    }
}
