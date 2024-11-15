package org.http4k.lens

import dev.forkhandles.values.ZonedDateTimeValue
import dev.forkhandles.values.ZonedDateTimeValueFactory
import java.time.ZoneId
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.util.Locale.ENGLISH

val Header.LAST_MODIFIED
    get() = Header.map(LastModified.Companion::parse, LastModified.Companion::show)
        .optional("Last-Modified")

class LastModified private constructor(value: ZonedDateTime) : ZonedDateTimeValue(value) {

    fun toHeaderValue() = LastModified.show(this)

    companion object : ZonedDateTimeValueFactory<LastModified>(
        ::LastModified, fmt = DateTimeFormatter
            .ofPattern("EEE, dd MMM yyyy HH:mm:ss z", ENGLISH)
            .withZone(ZoneId.of("GMT"))
    )
}
