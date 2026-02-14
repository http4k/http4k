package org.http4k.ai.a2a.model

import dev.forkhandles.values.NonBlankStringValueFactory
import dev.forkhandles.values.StringValue

class ContentMode private constructor(value: String) : StringValue(value) {
    companion object : NonBlankStringValueFactory<ContentMode>(::ContentMode) {
        val TEXT = of("text")
        val AUDIO = of("audio")
        val IMAGE = of("image")
        val VIDEO = of("video")
        val FILE = of("file")
    }
}
