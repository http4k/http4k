package org.http4k.connect.amazon.sts.model

import dev.forkhandles.values.StringValue
import dev.forkhandles.values.StringValueFactory
import dev.forkhandles.values.regex

class TokenCode private constructor(value: String) : StringValue(value) {
    companion object : StringValueFactory<TokenCode>(::TokenCode, "[\\d]{6}".regex)
}
