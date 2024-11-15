package org.http4k.connect.google.analytics.model

import dev.forkhandles.values.NonBlankStringValueFactory
import dev.forkhandles.values.StringValue

class UserAgent private constructor(value: String) : StringValue(value) {
    companion object : NonBlankStringValueFactory<UserAgent>(::UserAgent) {
        val Default =
            UserAgent.of("Mozilla/5.0 (Macintosh; Intel Mac OS X 10_10_4) AppleWebKit/600.7.12 (KHTML, like Gecko) Version/8.0.7 Safari/600.7.12")
    }
}
