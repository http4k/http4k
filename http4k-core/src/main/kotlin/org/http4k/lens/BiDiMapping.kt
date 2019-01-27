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

/**
 * A BiDiMapping defines a reusable bidirectional transformation between an input and output type
 */
class BiDiMapping<IN, OUT>(val clazz: Class<OUT>, private val asOut: (IN) -> OUT, private val asIn: (OUT) -> IN) {
    @JvmName("mapIn")
    fun map(out: OUT): IN = asIn(out)

    @JvmName("mapOut")
    fun map(asIn: IN): OUT = asOut(asIn)

    companion object {
        inline operator fun <IN, reified T> invoke(noinline read: (IN) -> T, noinline write: (T) -> IN) = BiDiMapping(T::class.java, read, write)
    }
}

fun BiDiMapping.Companion.int() = BiDiMapping(String::toInt, Int::toString)
fun BiDiMapping.Companion.long() = BiDiMapping(String::toLong, Long::toString)
fun BiDiMapping.Companion.double() = BiDiMapping(String::toDouble, Double::toString)
fun BiDiMapping.Companion.float() = BiDiMapping(String::toFloat, Float::toString)
fun BiDiMapping.Companion.boolean() = BiDiMapping(::safeBooleanFrom, Boolean::toString)
fun BiDiMapping.Companion.nonEmptyString() = BiDiMapping({ s: String -> if (s.isEmpty()) throw IllegalArgumentException() else s }, { it })
fun BiDiMapping.Companion.regex(pattern: String, group: Int = 1) = pattern.toRegex().run { BiDiMapping({ s: String -> matchEntire(s)?.groupValues?.get(group)!! }, { it }) }
fun BiDiMapping.Companion.regexObject() = BiDiMapping(::Regex, Regex::pattern)

fun BiDiMapping.Companion.duration() = BiDiMapping(Duration::parse, Duration::toString)
fun BiDiMapping.Companion.uri() = BiDiMapping(Uri.Companion::of, Uri::toString)
fun BiDiMapping.Companion.url() = BiDiMapping(::URL, URL::toExternalForm)
fun BiDiMapping.Companion.uuid() = BiDiMapping(UUID::fromString, UUID::toString)

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
        else -> throw IllegalArgumentException("illegal boolean")
    }