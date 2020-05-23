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

    val obj = ArbObject("hello", ArbObject("world", null, listOf(1), true), emptyList(), false)

    @Test
    fun `roundtrip arbitary object to and from string`() {
        val out = marshaller.asString(obj)
        assertThat(out, equalTo(expectedAutoMarshallingResult))
        assertThat(marshaller.asA(out, ArbObject::class), equalTo(obj))
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
            Status.OK
        )
        val out = marshaller.asString(obj)
        assertThat(out, equalTo(expectedAutoMarshallingResultPrimitives))
        assertThat(marshaller.asA(out, CommonJdkPrimitives::class), equalTo(obj))
    }

    @Test
    fun `roundtrip regex special as equals isn't comparable`() {
        val obj = RegexHolder(".*".toRegex())
        val out = marshaller.asString(obj)
        assertThat(out, equalTo("""{"regex":".*"}"""))
        assertThat(marshaller.asA(out, RegexHolder::class).regex.pattern, equalTo(obj.regex.pattern))
    }

    @Test
    fun `roundtrip wrapped map`() {
        val wrapper = MapHolder(mapOf("key" to "value", "key2" to "123"))
        assertThat(marshaller.asString(wrapper), equalTo("""{"value":{"key":"value","key2":"123"}}"""))
        assertThat(marshaller.asA(marshaller.asString(wrapper), MapHolder::class), equalTo(wrapper))
    }

    @Test
    fun `roundtrip custom number`() {
        val marshaller = customMarshaller()

        val wrapper = BigIntegerHolder(1.toBigInteger())
        assertThat(marshaller.asString(wrapper), equalTo("1"))
        assertThat(marshaller.asA("1", BigIntegerHolder::class), equalTo(wrapper))
    }

    @Test
    fun `roundtrip custom mapped number`() {
        val marshaller = customMarshaller()

        val wrapper = HolderHolder(MappedBigDecimalHolder(1.01.toBigDecimal()))
        assertThat(marshaller.asString(wrapper), equalTo("""{"value":"1.01"}"""))
        assertThat(marshaller.asA("""{"value":"1.01"}""", HolderHolder::class), equalTo(wrapper))
    }

    @Test
    fun `roundtrip custom decimal`() {
        val marshaller = customMarshaller()

        val wrapper = BigDecimalHolder(1.01.toBigDecimal())
        assertThat(marshaller.asString(wrapper), equalTo("1.01"))
        assertThat(marshaller.asA("1.01", BigDecimalHolder::class), equalTo(wrapper))
    }

    @Test
    fun `roundtrip custom boolean`() {
        val marshaller = customMarshaller()

        val wrapper = BooleanHolder(true)
        assertThat(marshaller.asString(wrapper), equalTo("true"))
        assertThat(marshaller.asA("true", BooleanHolder::class), equalTo(wrapper))
    }

    @Test
    fun `convert to inputstream`() {
        assertThat(marshaller.asInputStream(StringHolder("hello")).reader().use { it.readText() }, equalTo("""{"value":"hello"}"""))
    }

    @Test
    fun `throwable is marshalled`() {
        assertThat(marshaller.asString(ExceptionHolder(CustomException("foobar"))), startsWith("""{"value":"org.http4k.format.CustomException: foobar"""))
    }

    @Test
    fun `prohibit strings`() {
        val marshaller = customMarshaller()

        assertThat(marshaller.asString(StringHolder("hello")), equalTo("""{"value":"hello"}"""))

        assertThat({ marshaller.asA("""{"value":"hello"}""", StringHolder::class) }, throws<Exception>())
    }

    @Test
    open fun `fails decoding when a required value is null`() {
        assertThat({ marshaller.asA("{}", ArbObject::class) }, throws<Exception>())
    }

    @Test
    open fun `does not fail decoding when unknown value is encountered`() {
        assertThat(marshaller.asA("""{"value":"value","unknown":"ohno!"}""", StringHolder::class), equalTo(StringHolder("value")))
    }

    @Test
    fun `out only string`() {
        val marshaller = customMarshaller()

        val wrapper = OutOnlyHolder(OutOnly("foobar"))
        val actual = marshaller.asString(wrapper)
        assertThat(actual, equalTo("""{"value":"foobar"}"""))
        assertThat({ marshaller.asA(actual, OutOnlyHolder::class) }, throws<Exception>())
    }

    @Test
    fun `in only string`() {
        val marshaller = customMarshaller()

        val wrapper = InOnlyHolder(InOnly("foobar"))
        val expected = """{"value":"foobar"}"""
        assertThat({ marshaller.asString(wrapper) }, throws<IllegalArgumentException>())
        assertThat(marshaller.asA(expected, InOnlyHolder::class), equalTo(wrapper))
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
