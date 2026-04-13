package org.http4k.wiretap.util

import dev.forkhandles.values.StringValue
import dev.forkhandles.values.StringValueFactory

class HtmlContent private constructor(value: String) : StringValue(value) {
    companion object : StringValueFactory<HtmlContent>(::HtmlContent) {
        val empty = of("")
    }
}
