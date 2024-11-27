package org.http4k.connect.openai.auth

import dev.forkhandles.values.NonBlankStringValueFactory
import dev.forkhandles.values.StringValue

class OpenAIPluginId private constructor(value: String) : StringValue(value) {
    companion object : NonBlankStringValueFactory<OpenAIPluginId>(::OpenAIPluginId)
}
