package org.http4k.connect.model

import dev.forkhandles.values.NonBlankStringValueFactory
import dev.forkhandles.values.StringValue
import org.http4k.core.ContentType

class MimeType private constructor(value: String) : StringValue(value) {
    companion object : NonBlankStringValueFactory<MimeType>(::MimeType) {
        fun of(value: ContentType) = MimeType(value.withNoDirectives().value)

        val IMAGE_JPG = MimeType.of("image/jpeg")
        val IMAGE_PNG = MimeType.of("image/png")
        val IMAGE_GIF = MimeType.of("image/gif")
        val IMAGE_WEBP = MimeType.of("image/webp")
    }
}
