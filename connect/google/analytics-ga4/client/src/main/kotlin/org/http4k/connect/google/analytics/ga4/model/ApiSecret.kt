package org.http4k.connect.google.analytics.ga4.model

import dev.forkhandles.values.Maskers.hidden
import dev.forkhandles.values.NonBlankStringValueFactory
import dev.forkhandles.values.StringValue

class ApiSecret private constructor(value: String) : StringValue(value, hidden()) {
    companion object : NonBlankStringValueFactory<ApiSecret>(::ApiSecret)
}
