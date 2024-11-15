package org.http4k.connect.mattermost.model

import dev.forkhandles.values.StringValue
import dev.forkhandles.values.StringValueFactory
import dev.forkhandles.values.regex

class HexColour private constructor(value: String) : StringValue(value) {
    companion object : StringValueFactory<HexColour>(::HexColour, "#[0-9a-fA-F]{6}".regex)
}
