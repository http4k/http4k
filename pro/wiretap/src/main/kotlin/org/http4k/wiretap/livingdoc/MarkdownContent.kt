package org.http4k.wiretap.livingdoc

import dev.forkhandles.values.StringValue
import dev.forkhandles.values.StringValueFactory

class MarkdownContent private constructor(value: String) : StringValue(value) {
    companion object : StringValueFactory<MarkdownContent>(::MarkdownContent) {
        val empty = MarkdownContent("")
    }
}
