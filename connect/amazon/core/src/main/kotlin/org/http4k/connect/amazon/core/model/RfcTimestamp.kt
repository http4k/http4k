package org.http4k.connect.amazon.core.model

import dev.forkhandles.values.ZonedDateTimeValue
import dev.forkhandles.values.ZonedDateTimeValueFactory
import java.time.Instant
import java.time.ZoneId
import java.time.ZoneOffset
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter

class RfcTimestamp private constructor(value: ZonedDateTime): ZonedDateTimeValue(value) {
    companion object: ZonedDateTimeValueFactory<RfcTimestamp>(::RfcTimestamp, fmt = DateTimeFormatter.RFC_1123_DATE_TIME) {
        fun of(value: Instant, zone: ZoneId = ZoneOffset.UTC) = RfcTimestamp.of(ZonedDateTime.ofInstant(value, zone))
    }

    override fun toString() = show(this)
}
