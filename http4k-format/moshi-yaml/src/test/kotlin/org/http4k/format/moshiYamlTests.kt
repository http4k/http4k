package org.http4k.format

import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.equalTo
import com.squareup.moshi.Moshi.Builder
import org.http4k.format.StrictnessMode.FailOnUnknown
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test

class MoshiYamlAutoTest : AutoMarshallingContract(MoshiYaml) {
    override val expectedAutoMarshallingResult: String = "string:hello\n" +
        "child:\n" +
        "  string:world\n" +
        "  numbers:\n" +
        "  - 1.0\n" +
        "  bool:true\n" +
        "numbers:[\n" +
        "  ]\n" +
        "bool:false\n"

    override val expectedAutoMarshallingResultPrimitives: String = "duration:PT1S\n" +
        "localDate:'2000-01-01'\n" +
        "localTime:01:01:01\n" +
        "localDateTime:'2000-01-01T01:01:01'\n" +
        "zonedDateTime:2000-01-01T01:01:01Z[UTC]\n" +
        "offsetTime:01:01:01Z\n" +
        "offsetDateTime:'2000-01-01T01:01:01Z'\n" +
        "instant:'1970-01-01T00:00:00Z'\n" +
        "uuid:1a448854-1687-4f90-9562-7d527d64383c\n" +
        "uri:http://uri:8000\n" +
        "url:http://url:9000\n" +
        "status:200.0\n"

    override val expectedWrappedMap: String = "value:\n" +
        "  key:value\n" +
        "  key2:'123'\n"

    override val expectedConvertToInputStream: String = "value:hello\n"
    override val expectedThrowable: String = """value: "org.http4k.format.CustomException: foobar"""
    override val inputUnknownValue: String = """value: "value"
unknown: "2000-01-01"        
"""
    override val inputEmptyObject: String = """"""
    override val expectedRegexSpecial: String = """regex: .*
"""
    override val expectedMap = "key:value\n" +
        "key2:'123'\n"

    override val expectedArray = "[\"foo\",\"bar\",\"baz\"]\n"

    override val expectedAutoMarshallingZonesAndLocale = "zoneId:America/Toronto\nzoneOffset:-04:00\nlocale:en-CA\n"

    override fun strictMarshaller() = object : ConfigurableMoshiYaml(
        Builder().asConfigurable().customise(), strictness = FailOnUnknown
    ) {}

    override fun customMarshaller() = ConfigurableMoshiYaml(Builder().asConfigurable().customise()
        .add(NullSafeMapAdapter).add(ListAdapter))

    override fun customMarshallerProhibitStrings() = ConfigurableMoshiYaml(
        Builder().asConfigurable().prohibitStrings()
            .customise()
    )

    data class MapHolder(val map: Map<String, MapHolder?>)

    @Test
    fun `nulls are serialised for map entries`() {
        val input = MapHolder(mapOf("key" to null))
        assertThat(MoshiYaml.asFormatString(input), equalTo("map: {\n  }\n"))
    }

    @Disabled("not supported")
    override fun `throwable is marshalled`() {
    }

    @Disabled("not supported")
    override fun `exception is marshalled`() {
    }

    @Test
    override fun `roundtrip custom boolean`() {
        val marshaller = customMarshaller()

        val wrapper = BooleanHolder(true)
        assertThat(marshaller.asFormatString(wrapper), equalTo("'true'\n"))
        assertThat(marshaller.asA("true", BooleanHolder::class), equalTo(wrapper))
    }

    @Test
    override fun `roundtrip custom decimal`() {
        val marshaller = customMarshaller()

        val wrapper = BigDecimalHolder(1.01.toBigDecimal())
        assertThat(marshaller.asFormatString(wrapper), equalTo("'1.01'\n"))
        assertThat(marshaller.asA("1.01", BigDecimalHolder::class), equalTo(wrapper))
    }

    @Test
    override fun `roundtrip custom number`() {
        val marshaller = customMarshaller()

        val wrapper = BigIntegerHolder(1.toBigInteger())
        assertThat(marshaller.asFormatString(wrapper), equalTo("'1'\n"))
        assertThat(marshaller.asA("1", BigIntegerHolder::class), equalTo(wrapper))
    }

    @Test
    override fun `roundtrip custom value`() {
        val marshaller = customMarshaller()

        val wrapper = MyValueHolder(MyValue("foobar"))
        assertThat(marshaller.asFormatString(wrapper), equalTo("value: foobar\n"))
        assertThat(marshaller.asA("value: foobar\n", MyValueHolder::class), equalTo(wrapper))
        assertThat(marshaller.asA("value: \n", MyValueHolder::class), equalTo(MyValueHolder(null)))
    }

    @Test
    fun `on as a key is not quoted`() {
        val wrapper = mapOf("on" to "hello")
        assertThat(MoshiYaml.asFormatString(wrapper), equalTo("on: hello\n"))
    }

}
