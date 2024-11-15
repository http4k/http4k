package org.http4k.connect.mattermost.model

import dev.forkhandles.values.NonBlankStringValueFactory
import dev.forkhandles.values.StringValue

class EmojiName private constructor(value: String) : StringValue(value) {
    companion object : NonBlankStringValueFactory<EmojiName>(::EmojiName)
}
