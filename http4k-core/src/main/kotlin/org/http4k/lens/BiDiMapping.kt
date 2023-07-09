package org.http4k.lens

import org.http4k.base64Decoded
import org.http4k.base64Encode
import org.http4k.core.Credentials
import org.http4k.core.Uri
import org.http4k.events.EventCategory
import org.http4k.filter.SamplingDecision
import org.http4k.filter.TraceId
import org.http4k.urlDecoded
import org.http4k.urlEncoded
import java.io.PrintWriter
import java.io.StringWriter
import java.math.BigDecimal
import java.math.BigInteger
import java.net.URI
import java.net.URL
import java.time.Duration
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.OffsetDateTime
import java.time.OffsetTime
import java.time.YearMonth
import java.time.ZoneId
import java.time.ZoneOffset
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter
import java.time.format.DateTimeFormatter.ISO_INSTANT
import java.time.format.DateTimeFormatter.ISO_LOCAL_DATE
import java.time.format.DateTimeFormatter.ISO_LOCAL_DATE_TIME
import java.time.format.DateTimeFormatter.ISO_LOCAL_TIME
import java.time.format.DateTimeFormatter.ISO_OFFSET_DATE_TIME
import java.time.format.DateTimeFormatter.ISO_OFFSET_TIME
import java.time.format.DateTimeFormatter.ISO_ZONED_DATE_TIME
import java.util.Locale
import java.util.Locale.getDefault
import java.util.UUID

/**
 * A BiDiMapping defines a reusable bidirectional transformation between an input and output type
 */

open class BiDiMapping<IN, OUT>(val clazz: Class<OUT>, val asOut: (IN) -> OUT, val asIn: (OUT) -> IN) {

    inline fun <reified NEXT> map(crossinline nextOut: (OUT) -> NEXT, crossinline nextIn: (NEXT) -> OUT): BiDiMapping<IN, NEXT> =
        BiDiMapping(NEXT::class.java, { nextOut(asOut(it)) }, { asIn(nextIn(it)) })

    @JvmName("asIn")
    operator fun invoke(out: OUT): IN = asIn(out)

    @JvmName("asOut")
    operator fun invoke(asIn: IN): OUT = asOut(asIn)

    companion object {
        inline operator fun <IN, reified T> invoke(noinline asOut: (IN) -> T, noinline asIn: (T) -> IN) = BiDiMapping(T::class.java, asOut, asIn)
    }
}

/**
 * A set of standardised String <-> Type conversions which are used throughout http4k
 */
object StringBiDiMappings {
    fun int() = BiDiMapping(String::toInt, Int::toString)
    fun long() = BiDiMapping(String::toLong, Long::toString)
    fun double() = BiDiMapping(String::toDouble, Double::toString)
    fun float() = BiDiMapping(String::toFloat, Float::toString)
    fun bigDecimal() = BiDiMapping(String::toBigDecimal, BigDecimal::toString)
    fun bigInteger() = BiDiMapping(String::toBigInteger, BigInteger::toString)
    fun boolean() = BiDiMapping(String::asSafeBoolean, Boolean::toString)
    fun nonEmpty() = BiDiMapping({ s: String -> s.ifEmpty { throw IllegalArgumentException("String cannot be empty") } }, { it })
    fun regex(pattern: String, group: Int = 1) = pattern.toRegex().run { BiDiMapping({ s: String -> matchEntire(s)?.groupValues?.get(group)!! }, { it }) }
    fun regexObject() = BiDiMapping(::Regex, Regex::pattern)
    fun urlEncoded() = BiDiMapping(String::urlDecoded, String::urlEncoded)
    fun duration() = BiDiMapping(Duration::parse, Duration::toString)
    fun uri() = BiDiMapping(Uri.Companion::of, Uri::toString)
    fun url() = BiDiMapping({ URI(it).toURL() }, URL::toExternalForm)
    fun uuid() = BiDiMapping(UUID::fromString, UUID::toString)
    fun base64() = BiDiMapping(String::base64Decoded, String::base64Encode)

    fun instant() = BiDiMapping(Instant::parse, ISO_INSTANT::format)
    fun yearMonth(formatter: DateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM")) = BiDiMapping({ YearMonth.parse(it, formatter) }, formatter::format)
    fun localTime(formatter: DateTimeFormatter = ISO_LOCAL_TIME) = BiDiMapping({ LocalTime.parse(it, formatter) }, formatter::format)
    fun localDate(formatter: DateTimeFormatter = ISO_LOCAL_DATE) = BiDiMapping({ LocalDate.parse(it, formatter) }, formatter::format)
    fun localDateTime(formatter: DateTimeFormatter = ISO_LOCAL_DATE_TIME) = BiDiMapping({ LocalDateTime.parse(it, formatter) }, formatter::format)
    fun zonedDateTime(formatter: DateTimeFormatter = ISO_ZONED_DATE_TIME) = BiDiMapping({ ZonedDateTime.parse(it, formatter) }, formatter::format)
    fun offsetTime(formatter: DateTimeFormatter = ISO_OFFSET_TIME) = BiDiMapping({ OffsetTime.parse(it, formatter) }, formatter::format)
    fun offsetDateTime(formatter: DateTimeFormatter = ISO_OFFSET_DATE_TIME) = BiDiMapping({ OffsetDateTime.parse(it, formatter) }, formatter::format)
    fun zoneId() = BiDiMapping(ZoneId::of, ZoneId::getId)
    fun zoneOffset() = BiDiMapping(ZoneOffset::of, ZoneOffset::getId)
    fun eventCategory() = BiDiMapping(::EventCategory, EventCategory::toString)
    fun traceId() = BiDiMapping(::TraceId, TraceId::value)
    fun samplingDecision() = BiDiMapping(::SamplingDecision, SamplingDecision::value)
    fun throwable() = BiDiMapping({ throw Exception(it) }, Throwable::asString)
    fun locale() = BiDiMapping(
        { s -> Locale.forLanguageTag(s).takeIf { it.language.isNotEmpty() } ?: throw IllegalArgumentException("Could not parse IETF locale") },
        Locale::toLanguageTag
    )
    fun basicCredentials() = BiDiMapping(
        { value -> value.trim()
            .takeIf { value.startsWith("Basic") }
            ?.substringAfter("Basic")
            ?.trim()
            ?.safeBase64Decoded()
            ?.split(":", ignoreCase = false, limit = 2)
            .let { Credentials(it?.getOrNull(0) ?: "", it?.getOrNull(1) ?: "") }
        },
        { credentials: Credentials -> "Basic ${"${credentials.user}:${credentials.password}".base64Encode()}" }
    )
    inline fun <reified T : Enum<T>> enum() = BiDiMapping<String, T>(::enumValueOf, Enum<T>::name)
    inline fun <reified T : Enum<T>> caseInsensitiveEnum() = BiDiMapping(
        { text -> enumValues<T>().first { it.name.equals(text, ignoreCase = true) } },
        Enum<T>::name
    )

    fun <T> csv(delimiter: String = ",", mapElement: BiDiMapping<String, T>) = BiDiMapping<String, List<T>>(
        asOut = { if (it.isEmpty()) emptyList() else it.split(delimiter).map(mapElement::invoke) },
        asIn = { it.joinToString(delimiter, transform = mapElement::invoke) }
    )

    private fun String.safeBase64Decoded(): String? = try {
        base64Decoded()
    } catch (e: IllegalArgumentException) { null }
}

internal fun Throwable.asString() = StringWriter().use { output -> PrintWriter(output).use { printer -> printStackTrace(printer); output.toString() } }

internal fun String.asSafeBoolean(): Boolean =
    when {
        uppercase(getDefault()) == "TRUE" -> true
        uppercase(getDefault()) == "FALSE" -> false
        else -> throw IllegalArgumentException("illegal boolean: $this}")
    }
