package org.http4k.format

import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.equalTo
import com.natpryce.hamkrest.startsWith
import com.natpryce.hamkrest.throws
import org.http4k.core.Status
import org.http4k.core.Status.Companion.OK
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

abstract class AutoMarshallingContract(private val j: AutoMarshalling) {

    protected open val expectedAutoMarshallingResult = """{"string":"hello","child":{"string":"world","child":null,"numbers":[1],"bool":true},"numbers":[],"bool":false}"""
    protected open val expectedAutoMarshallingResultPrimitives = """{"duration":"PT1S","localDate":"2000-01-01","localTime":"01:01:01","localDateTime":"2000-01-01T01:01:01","zonedDateTime":"2000-01-01T01:01:01Z[UTC]","offsetTime":"01:01:01Z","offsetDateTime":"2000-01-01T01:01:01Z","instant":"1970-01-01T00:00:00Z","uuid":"1a448854-1687-4f90-9562-7d527d64383c","uri":"http://uri:8000","url":"http://url:9000","status":200}"""

    val obj = ArbObject("hello", ArbObject("world", null, listOf(1), true), emptyList(), false)

    @Test
    fun `roundtrip arbitary object to and from JSON string`() {
        val out = j.asString(obj)
        assertThat(out, equalTo(expectedAutoMarshallingResult))
        assertThat(j.asA(out, ArbObject::class), equalTo(obj))
    }

    @Test
    open fun `fails decoding when a required value is null`() {
        assertThat({ j.asA("{}", ArbObject::class) }, throws<Exception>())
    }

    @Test
    open fun `does not fail decoding when unknown value is encountered`() {
        assertThat(j.asA("""{"value":"value","unknown":"ohno!"}""", StringHolder::class), equalTo(StringHolder("value")))
    }

    @Test
    fun `roundtrip object with common java primitive types`() {
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
            OK
        )
        val out = j.asString(obj)
        assertThat(out, equalTo(expectedAutoMarshallingResultPrimitives))
        assertThat(j.asA(out, CommonJdkPrimitives::class), equalTo(obj))
    }

    @Test
    fun `roundtrip regex special as equals isn't comparable`() {
        val obj = RegexHolder(".*".toRegex())
        val out = j.asString(obj)
        assertThat(out, equalTo("""{"regex":".*"}"""))
        assertThat(j.asA(out, RegexHolder::class).regex.pattern, equalTo(obj.regex.pattern))
    }

    @Test
    fun `roundtrip wrapped map`() {
        val wrapper = MapHolder(mapOf("key" to "value", "key2" to "123"))
        assertThat(j.asString(wrapper), equalTo("""{"value":{"key":"value","key2":"123"}}"""))
        assertThat(j.asA(j.asString(wrapper), MapHolder::class), equalTo(wrapper))
    }

    @Test
    fun `roundtrip custom number`() {
        val json = customJson()

        val wrapper = BigIntegerHolder(1.toBigInteger())
        assertThat(json.asString(wrapper), equalTo("1"))
        assertThat(json.asA("1", BigIntegerHolder::class), equalTo(wrapper))
    }

    @Test
    fun `out only string`() {
        val json = customJson()

        val wrapper = OutOnlyHolder(OutOnly("foobar"))
        val actual = json.asString(wrapper)
        assertThat(actual, equalTo("""{"value":"foobar"}"""))
        assertThat({ json.asA(actual, OutOnlyHolder::class) }, throws<Exception>())
    }

    @Test
    fun `in only string`() {
        val json = customJson()

        val wrapper = InOnlyHolder(InOnly("foobar"))
        val expected = """{"value":"foobar"}"""
        assertThat({ json.asString(wrapper) }, throws<IllegalArgumentException>())
        assertThat(json.asA(expected, InOnlyHolder::class), equalTo(wrapper))
    }

    @Test
    fun `roundtrip custom mapped number`() {
        val json = customJson()

        val wrapper = HolderHolder(MappedBigDecimalHolder(1.01.toBigDecimal()))
        assertThat(json.asString(wrapper), equalTo("""{"value":"1.01"}"""))
        assertThat(json.asA("""{"value":"1.01"}""", HolderHolder::class), equalTo(wrapper))
    }

    @Test
    fun `roundtrip custom decimal`() {
        val json = customJson()

        val wrapper = BigDecimalHolder(1.01.toBigDecimal())
        assertThat(json.asString(wrapper), equalTo("1.01"))
        assertThat(json.asA("1.01", BigDecimalHolder::class), equalTo(wrapper))
    }

    @Test
    fun `roundtrip custom boolean`() {
        val json = customJson()

        val wrapper = BooleanHolder(true)
        assertThat(json.asString(wrapper), equalTo("true"))
        assertThat(json.asA("true", BooleanHolder::class), equalTo(wrapper))
    }

    @Test
    fun `prohibit strings`() {
        val json = customJson()

        assertThat(json.asString(StringHolder("hello")), equalTo("""{"value":"hello"}"""))

        assertThat({ json.asA("""{"value":"hello"}""", StringHolder::class) }, throws<Exception>())
    }

    @Test
    fun `convert to inputstream`() {
        assertThat(j.asInputStream(StringHolder("hello")).reader().use { it.readText() }, equalTo("""{"value":"hello"}"""))
    }

    @Test
    fun `throwable is marshalled`() {
        assertThat(j.asString(ExceptionHolder(CustomException("foobar"))), startsWith("""{"value":"org.http4k.format.CustomException: foobar"""))
    }

    abstract fun customJson(): AutoMarshallingJson
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

