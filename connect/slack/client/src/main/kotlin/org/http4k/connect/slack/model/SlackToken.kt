package org.http4k.connect.slack.model

import dev.forkhandles.values.NonBlankStringValueFactory
import dev.forkhandles.values.StringValue

class SlackToken private constructor(value: String) : StringValue(value) {
    companion object : NonBlankStringValueFactory<SlackToken>(::SlackToken)
}
