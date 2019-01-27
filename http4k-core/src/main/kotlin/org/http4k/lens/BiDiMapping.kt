package org.http4k.lens

import org.http4k.core.Uri
import java.net.URL
import java.time.Duration
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.OffsetDateTime
import java.time.OffsetTime
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeFormatter.ISO_INSTANT
import java.time.format.DateTimeFormatter.ISO_LOCAL_DATE
import java.time.format.DateTimeFormatter.ISO_LOCAL_DATE_TIME
import java.time.format.DateTimeFormatter.ISO_LOCAL_TIME
import java.time.format.DateTimeFormatter.ISO_OFFSET_DATE_TIME
import java.time.format.DateTimeFormatter.ISO_OFFSET_TIME
import java.time.format.DateTimeFormatter.ISO_ZONED_DATE_TIME
import java.util.UUID

data class BiDiMapping<T>(val read: (String) -> T, val write: (T) -> String = { it.toString() }) {
    companion object
}

fun BiDiMapping.Companion.int() = BiDiMapping(String::toInt, kotlin.Int::toString)
fun BiDiMapping.Companion.long() = BiDiMapping(String::toLong, kotlin.Long::toString)
fun BiDiMapping.Companion.double() = BiDiMapping(String::toDouble, kotlin.Double::toString)
fun BiDiMapping.Companion.float() = BiDiMapping(String::toFloat, kotlin.Float::toString)
fun BiDiMapping.Companion.boolean() = BiDiMapping(::safeBooleanFrom, kotlin.Boolean::toString)
fun BiDiMapping.Companion.nonEmptyString() = BiDiMapping({ if (it.isEmpty()) throw IllegalArgumentException() else it })
fun BiDiMapping.Companion.regex(pattern: String, group: Int = 1) = pattern.toRegex().run { BiDiMapping({ matchEntire(it)?.groupValues?.get(group)!! }) }
fun BiDiMapping.Companion.regexObject() = BiDiMapping(::Regex, Regex::pattern)

fun BiDiMapping.Companion.duration() = BiDiMapping(Duration::parse)
fun BiDiMapping.Companion.uri() = BiDiMapping(Uri.Companion::of)
fun BiDiMapping.Companion.url() = BiDiMapping(::URL, URL::toExternalForm)
fun BiDiMapping.Companion.uuid() = BiDiMapping(UUID::fromString)

fun BiDiMapping.Companion.instant() = BiDiMapping(Instant::parse, ISO_INSTANT::format)
fun BiDiMapping.Companion.localTime(formatter: DateTimeFormatter = ISO_LOCAL_TIME) = BiDiMapping({ LocalTime.parse(it, formatter) }, formatter::format)
fun BiDiMapping.Companion.localDate(formatter: DateTimeFormatter = ISO_LOCAL_DATE) = BiDiMapping({ LocalDate.parse(it, formatter) }, formatter::format)
fun BiDiMapping.Companion.localDateTime(formatter: DateTimeFormatter = ISO_LOCAL_DATE_TIME) = BiDiMapping({ LocalDateTime.parse(it, formatter) }, formatter::format)
fun BiDiMapping.Companion.zonedDateTime(formatter: DateTimeFormatter = ISO_ZONED_DATE_TIME) = BiDiMapping({ ZonedDateTime.parse(it, formatter) }, formatter::format)
fun BiDiMapping.Companion.offsetTime(formatter: DateTimeFormatter = ISO_OFFSET_TIME) = BiDiMapping({ OffsetTime.parse(it, formatter) }, formatter::format)
fun BiDiMapping.Companion.offsetDateTime(formatter: DateTimeFormatter = ISO_OFFSET_DATE_TIME) = BiDiMapping({ OffsetDateTime.parse(it, formatter) }, formatter::format)

internal fun safeBooleanFrom(value: String): Boolean =
    when {
        value.toUpperCase() == "TRUE" -> true
        value.toUpperCase() == "FALSE" -> false
        else -> throw kotlin.IllegalArgumentException("illegal boolean")
    }