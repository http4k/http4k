package org.http4k.mcp

import dev.forkhandles.values.NonBlankStringValueFactory
import dev.forkhandles.values.StringValue
import org.http4k.core.ContentType

class MimeType private constructor(value: String) : StringValue(value) {
    companion object : NonBlankStringValueFactory<MimeType>(::MimeType) {
        fun of(value: ContentType) = MimeType(value.withNoDirectives().value)
    }
}
