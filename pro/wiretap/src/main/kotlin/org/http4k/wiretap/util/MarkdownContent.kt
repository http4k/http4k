package org.http4k.wiretap.util

import dev.forkhandles.values.StringValue
import dev.forkhandles.values.StringValueFactory

class MarkdownContent private constructor(value: String) : StringValue(value) {
    companion object : StringValueFactory<MarkdownContent>(::MarkdownContent) {
        val empty = of("")
    }
}
