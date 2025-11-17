package org.http4k.connect.model

import dev.forkhandles.values.NonBlankStringValueFactory
import dev.forkhandles.values.StringValue
import org.http4k.core.ContentType

class MimeType private constructor(value: String) : StringValue(value) {
    companion object : NonBlankStringValueFactory<MimeType>(::MimeType) {
        fun of(value: ContentType) = MimeType(value.withNoDirectives().value)

        val TEXT_PLAIN = of("text/plain")
        val IMAGE_JPG = of("image/jpeg")
        val IMAGE_PNG = of("image/png")
        val IMAGE_GIF = of("image/gif")
        val IMAGE_WEBP = of("image/webp")
        val APPLICATION_PDF = of("application/pdf")
        val APPLICATION_JSON = of("application/json")
    }
}
