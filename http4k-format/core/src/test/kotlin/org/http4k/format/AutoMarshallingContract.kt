package org.http4k.format

import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.equalTo
import com.natpryce.hamkrest.startsWith
import com.natpryce.hamkrest.throws
import org.http4k.core.Status
import org.http4k.core.Uri
import org.http4k.lens.StringBiDiMappings
import org.junit.jupiter.api.Test
import java.math.BigDecimal
import java.math.BigInteger
import java.net.URL
import java.time.Duration
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.LocalTime
import java.time.OffsetDateTime
import java.time.OffsetTime
import java.time.ZoneId
import java.time.ZoneOffset
import java.time.ZonedDateTime
import java.util.UUID

data class CommonJdkPrimitives(
    val duration: Duration,
    val localDate: LocalDate,
    val localTime: LocalTime,
    val localDateTime: LocalDateTime,
    val zonedDateTime: ZonedDateTime,
    val offsetTime: OffsetTime,
    val offsetDateTime: OffsetDateTime,
    val instant: Instant,
    val uuid: UUID,
    val uri: Uri,
    val url: URL,
    val status: Status
)

data class ArbObject(val string: String, val child: ArbObject?, val numbers: List<Int>, val bool: Boolean)

data class RegexHolder(val regex: Regex)

data class StringHolder(val value: String)
data class AnotherString(val value: String)
data class BooleanHolder(val value: Boolean)
data class MapHolder(val value: Map<String, Any>)
data class BigDecimalHolder(val value: BigDecimal)
data class BigIntegerHolder(val value: BigInteger)
data class MappedBigDecimalHolder(val value: BigDecimal)
data class HolderHolder(val value: MappedBigDecimalHolder)
data class OutOnlyHolder(val value: OutOnly)
data class OutOnly(val value: String)
data class InOnlyHolder(val value: InOnly)
data class InOnly(val value: String)
data class ExceptionHolder(val value: Throwable)
class CustomException(m: String) : RuntimeException(m)

abstract class AutoMarshallingContract(private val marshaller: AutoMarshalling) {

    protected abstract val expectedAutoMarshallingResult: String
    protected abstract val expectedAutoMarshallingResultPrimitives: String
    protected abstract val expectedWrappedMap: String
    protected abstract val expectedMap: String
    protected abstract val expectedConvertToInputStream: String
    protected abstract val expectedThrowable: String
    protected abstract val inputUnknownValue: String
    protected abstract val inputEmptyObject: String
    protected abstract val expectedRegexSpecial: String


    val obj = ArbObject("hello", ArbObject("world", null, listOf(1), true), emptyList(), false)

    @Test
    open fun `roundtrip arbitrary object to and from string`() {
        val out = marshaller.asFormatString(obj)
        assertThat(out.normaliseJson(), equalTo(expectedAutoMarshallingResult.normaliseJson()))
        assertThat(marshaller.asA(out, ArbObject::class), equalTo(obj))
    }

    @Test
    open fun `roundtrip arbitrary object through convert`() {
        assertThat(marshaller.convert(obj), equalTo(obj))
    }

    @Test
    open fun `roundtrip arbitrary object to and from inputstream`() {
        val out = marshaller.asFormatString(obj)
        assertThat(out.normaliseJson(), equalTo(expectedAutoMarshallingResult.normaliseJson()))
        assertThat(marshaller.asA(out.byteInputStream(), ArbObject::class), equalTo(obj))
    }

    @Test
    open fun `roundtrip object with common java primitive types`() {
        val localDate = LocalDate.of(2000, 1, 1)
        val localTime = LocalTime.of(1, 1, 1)
        val zoneOffset = ZoneOffset.UTC
        val obj = CommonJdkPrimitives(
            Duration.ofMillis(1000),
            localDate,
            localTime,
            LocalDateTime.of(localDate, localTime),
            ZonedDateTime.of(localDate, localTime, ZoneId.of("UTC")),
            OffsetTime.of(localTime, zoneOffset),
            OffsetDateTime.of(localDate, localTime, zoneOffset),
            Instant.EPOCH,
            UUID.fromString("1a448854-1687-4f90-9562-7d527d64383c"),
            Uri.of("http://uri:8000"),
            URL("http://url:9000"),
            Status.OK
        )
        val out = marshaller.asFormatString(obj)
        assertThat(out.normaliseJson(), equalTo(expectedAutoMarshallingResultPrimitives.normaliseJson()))
        assertThat(marshaller.asA(out, CommonJdkPrimitives::class), equalTo(obj))
    }

    @Test
    open fun `roundtrip regex special as equals isn't comparable`() {
        val obj = RegexHolder(".*".toRegex())
        val out = marshaller.asFormatString(obj)
        assertThat(out.normaliseJson(), equalTo(expectedRegexSpecial.normaliseJson()))
        assertThat(marshaller.asA(out, RegexHolder::class).regex.pattern, equalTo(obj.regex.pattern))
    }

    @Test
    open fun `roundtrip map`() {
        val wrapper = mapOf("key" to "value", "key2" to "123")
        val asString = marshaller.asFormatString(wrapper)
        assertThat(asString.normaliseJson(), equalTo(expectedMap))
        assertThat(marshaller.asA(asString), equalTo(wrapper))
    }

    @Test
    open fun `roundtrip wrapped map`() {
        val wrapper = MapHolder(mapOf("key" to "value", "key2" to "123"))
        assertThat(marshaller.asFormatString(wrapper).normaliseJson(), equalTo(expectedWrappedMap.normaliseJson()))
        assertThat(marshaller.asA(marshaller.asFormatString(wrapper), MapHolder::class), equalTo(wrapper))
    }

    @Test
    open fun `roundtrip custom number`() {
        val marshaller = customMarshaller()

        val wrapper = BigIntegerHolder(1.toBigInteger())
        assertThat(marshaller.asFormatString(wrapper), equalTo("1"))
        assertThat(marshaller.asA("1", BigIntegerHolder::class), equalTo(wrapper))
    }

    @Test
    open fun `roundtrip custom decimal`() {
        val marshaller = customMarshaller()

        val wrapper = BigDecimalHolder(1.01.toBigDecimal())
        assertThat(marshaller.asFormatString(wrapper), equalTo("1.01"))
        assertThat(marshaller.asA("1.01", BigDecimalHolder::class), equalTo(wrapper))
    }

    @Test
    open fun `roundtrip custom boolean`() {
        val marshaller = customMarshaller()

        val wrapper = BooleanHolder(true)
        assertThat(marshaller.asFormatString(wrapper), equalTo("true"))
        assertThat(marshaller.asA("true", BooleanHolder::class), equalTo(wrapper))
    }

    @Test
    open fun `convert to inputstream`() {
        assertThat(marshaller.asInputStream(StringHolder("hello")).reader().use { it.readText() }
            .normaliseJson(), equalTo(expectedConvertToInputStream.normaliseJson()))
    }

    @Test
    open fun `throwable is marshalled`() {
        assertThat(
            marshaller.asFormatString(ExceptionHolder(CustomException("foobar"))).normaliseJson(),
            startsWith(expectedThrowable.normaliseJson())
        )
    }

    @Test
    open fun `fails decoding when a required value is null`() {
        assertThat({ marshaller.asA(inputEmptyObject, ArbObject::class) }, throws<Exception>())
    }

    @Test
    open fun `does not fail decoding when unknown value is encountered`() {
        assertThat(marshaller.asA(inputUnknownValue, StringHolder::class), equalTo(StringHolder("value")))
    }

    abstract fun customMarshaller(): AutoMarshalling
}

fun <T> AutoMappingConfiguration<T>.customise(): T = prohibitStrings()
    .bigDecimal(::BigDecimalHolder, BigDecimalHolder::value)
    .bigInteger(::BigIntegerHolder, BigIntegerHolder::value)
    .boolean(::BooleanHolder, BooleanHolder::value)
    .text(::AnotherString, AnotherString::value)
    .text(OutOnly::value)
    .text(::InOnly)
    .uuid({ it }, { it })
    .uri({ it }, { it })
    .instant({ it }, { it })
    .localDateTime({ it }, { it })
    .localDate({ it }, { it })
    .localTime({ it }, { it })
    .offsetDateTime({ it }, { it })
    .offsetTime({ it }, { it })
    .yearMonth({ it }, { it })
    .zonedDateTime({ it }, { it })
    .text(StringBiDiMappings.bigDecimal().map(::MappedBigDecimalHolder, MappedBigDecimalHolder::value))
    .done()

fun String.normaliseJson() = replace(" : ", ":").replace(": ", ":").replace(", ", ",")
