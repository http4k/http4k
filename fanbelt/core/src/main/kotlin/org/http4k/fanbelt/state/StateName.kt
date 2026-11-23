package org.http4k.fanbelt.state

import dev.forkhandles.values.StringValue
import dev.forkhandles.values.StringValueFactory
import dev.forkhandles.values.regex

class StateName private constructor(value: String) : StringValue(value) {
    companion object : StringValueFactory<StateName>(::StateName, "[a-z][A-Za-z0-9-]{0,60}".regex)
}
