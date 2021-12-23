package org.http4k.format

import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.equalTo
import com.natpryce.hamkrest.throws
import kotlinx.serialization.Contextual
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.Serializable
import org.http4k.core.Body
import org.http4k.core.Response
import org.http4k.core.Status
import org.http4k.core.Uri
import org.http4k.core.with
import org.http4k.format.KotlinxSerialization.auto
import org.http4k.lens.StringBiDiMappings
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test
import java.math.BigDecimal
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

@Serializable
data class MapHolder(val value: Map<String, String>)

@Serializable
data class ArbObject(val string: String, val child: ArbObject?, val numbers: List<Int>, val bool: Boolean)

@Serializable
data class CommonJdkPrimitives(
    @Contextual
    val duration: Duration,
    @Contextual
    val localDate: LocalDate,
    @Contextual
    val localTime: LocalTime,
    @Contextual
    val localDateTime: LocalDateTime,
    @Contextual
    val zonedDateTime: ZonedDateTime,
    @Contextual
    val offsetTime: OffsetTime,
    @Contextual
    val offsetDateTime: OffsetDateTime,
    @Contextual
    val instant: Instant,
    @Contextual
    val uuid: UUID,
    @Contextual
    val uri: Uri,
    @Contextual
    val url: URL,
    @Contextual
    val status: Status
)

@Serializable
data class RegexHolder(@Contextual val regex: Regex)

@Serializable
data class StringHolder(@Contextual val value: String)

@Serializable
data class OutOnlyHolder(@Contextual val value: OutOnly)

@Serializable
data class InOnlyHolder(@Contextual val value: InOnly)

@Serializable
data class HolderHolder(@Contextual val value: MappedBigDecimalHolder)

data class MappedBigDecimalHolder(val value: BigDecimal)

@Serializable
sealed class PolymorphicParent

@Serializable
data class FirstChild(val something: String) : PolymorphicParent()

@Serializable
data class SecondChild(val somethingElse: String) : PolymorphicParent()

class KotlinxSerializationAutoTest : AutoMarshallingJsonContract(KotlinxSerialization) {
    @Test
    override fun `roundtrip arbitrary object to and from string`() {
        val obj = ArbObject("hello", ArbObject("world", null, listOf(1), true), emptyList(), false)
        val out = KotlinxSerialization.asFormatString(obj)
        assertThat(out, equalTo(expectedAutoMarshallingResult))
        assertThat(KotlinxSerialization.asA(out, ArbObject::class), equalTo(obj))
    }

    override fun `roundtrip arbitrary object through convert`() {
        val obj = ArbObject("hello", ArbObject("world", null, listOf(1), true), emptyList(), false)
        assertThat(KotlinxSerialization.convert(obj), equalTo(obj))
    }

    @Test
    override fun `roundtrip object with common java primitive types`() {
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
        val out = KotlinxSerialization.asFormatString(obj)
        assertThat(out, equalTo(expectedAutoMarshallingResultPrimitives))
        assertThat(KotlinxSerialization.asA(out, CommonJdkPrimitives::class), equalTo(obj))
    }

    @Test
    override fun `roundtrip regex special as equals isn't comparable`() {
        val obj = RegexHolder(".*".toRegex())
        val out = KotlinxSerialization.asFormatString(obj)
        assertThat(out, equalTo(expectedRegexSpecial))
        assertThat(KotlinxSerialization.asA(out, RegexHolder::class).regex.pattern, equalTo(obj.regex.pattern))
    }

    @Test
    override fun `roundtrip wrapped map`() {
        val wrapper = MapHolder(mapOf("key" to "value", "key2" to "123"))
        assertThat(KotlinxSerialization.asFormatString(wrapper), equalTo(expectedWrappedMap))
        assertThat(
            KotlinxSerialization.asA(KotlinxSerialization.asFormatString(wrapper), MapHolder::class),
            equalTo(wrapper)
        )
    }

    @Test
    @Disabled("kotlinx.serialization does not BigInteger")
    override fun `roundtrip custom number`() {
        super.`roundtrip custom number`()
    }

    @Test
    @Disabled("kotlinx.serialization does not support BigDecimal")
    override fun `roundtrip custom decimal`() {
        super.`roundtrip custom decimal`()
    }

    @Test
    override fun `convert to inputstream`() {
        assertThat(
            KotlinxSerialization.asInputStream(StringHolder("hello")).reader().use { it.readText() },
            equalTo(expectedConvertToInputStream)
        )
    }

    @Disabled()
    override fun `roundtrip custom value`() {
    }

    @Test
    @Disabled("kotlinx.serialization does not support serialization auto-fallback to parent class")
    override fun `throwable is marshalled`() {
    }

    @Test
    @Disabled("kotlinx.serialization does not support serialization auto-fallback to parent class")
    override fun `exception is marshalled`() {
    }

    @Test
    @Disabled("kotlinx.serialization does not support default serialization of LinkedHashMap")
    override fun `roundtrip map`() {
    }

    @Test
    override fun `fails decoding when a required value is null`() {
        assertThat({ KotlinxSerialization.asA(inputEmptyObject, ArbObject::class) }, throws<Exception>())
    }

    @Test
    override fun `does not fail decoding when unknown value is encountered`() {
        assertThat(KotlinxSerialization.asA(inputUnknownValue, StringHolder::class), equalTo(StringHolder("value")))
    }

    @Test
    override fun `out only string`() {
        val marshaller = customMarshaller()

        val wrapper = OutOnlyHolder(OutOnly("foobar"))
        val actual = marshaller.asFormatString(wrapper)
        assertThat(actual, equalTo(expectedInOutOnly))
        assertThat({ marshaller.asA(actual, OutOnlyHolder::class) }, throws<Exception>())
    }

    @Test
    override fun `in only string`() {
        val marshaller = customMarshaller()

        val wrapper = InOnlyHolder(InOnly("foobar"))
        assertThat({ marshaller.asFormatString(wrapper) }, throws<Exception>())
        assertThat(marshaller.asA(expectedInOutOnly, InOnlyHolder::class), equalTo(wrapper))
    }

    @Test
    override fun `prohibit strings`() {
        val marshaller = customMarshallerProhibitStrings()

        assertThat(marshaller.asFormatString(StringHolder("hello")), equalTo(expectedConvertToInputStream))
        assertThat({ marshaller.asA(expectedConvertToInputStream, StringHolder::class) }, throws<Exception>())
    }

    @Test
    override fun `roundtrip custom mapped number`() {
        val marshaller = customMarshaller()

        val wrapper = HolderHolder(MappedBigDecimalHolder(1.01.toBigDecimal()))
        assertThat(marshaller.asFormatString(wrapper), equalTo(expectedCustomWrappedNumber))
        assertThat(marshaller.asA(expectedCustomWrappedNumber, HolderHolder::class), equalTo(wrapper))
    }

    @ExperimentalSerializationApi
    @Test
    fun `roundtrip arbitrary object to and from JSON element`() {
        val obj = ArbObject("hello", ArbObject("world", null, listOf(1), true), emptyList(), false)
        val out = KotlinxSerialization.asJsonObject(obj)
        assertThat(KotlinxSerialization.asA(out, ArbObject::class), equalTo(obj))
    }

    @Test
    fun `roundtrip list of arbitrary objects to and from body`() {
        val body = Body.auto<List<ArbObject>>().toLens()

        val obj = ArbObject("hello", ArbObject("world", null, listOf(1), true), emptyList(), false)

        assertThat(body(Response(Status.OK).with(body of listOf(obj))), equalTo(listOf(obj)))
    }

    @Test
    fun `roundtrip array of arbitrary objects to and from body`() {
        val body = Body.auto<Array<ArbObject>>().toLens()

        val obj = ArbObject("hello", ArbObject("world", null, listOf(1), true), emptyList(), false)

        assertThat(body(Response(Status.OK).with(body of arrayOf(obj))).toList(), equalTo(listOf(obj)))
    }

    @Test
    fun `roundtrip polymorphic object to and from body`() {
        val body = Body.auto<PolymorphicParent>().toLens()

        val firstChild: PolymorphicParent = FirstChild("hello")
        val secondChild: PolymorphicParent = SecondChild("world")

        assertThat(body(Response(Status.OK).with(body of firstChild)), equalTo(firstChild))
        assertThat(body(Response(Status.OK).with(body of secondChild)), equalTo(secondChild))
    }

    @Test
    fun `roundtrip list of polymorphic objects to and from body`() {
        val body = Body.auto<List<PolymorphicParent>>().toLens()

        val list = listOf(FirstChild("hello"), SecondChild("world"))

        assertThat(body(Response(Status.OK).with(body of list)), equalTo(list))
    }

    override fun customMarshaller(): AutoMarshalling =
        object : ConfigurableKotlinxSerialization({
            asConfigurable()
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
        }) {}

    override fun customMarshallerProhibitStrings() = object : ConfigurableKotlinxSerialization({
        asConfigurable()
            .prohibitStrings()
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
    }) {}
}
