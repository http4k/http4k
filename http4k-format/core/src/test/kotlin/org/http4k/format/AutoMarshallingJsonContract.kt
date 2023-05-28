package org.http4k.format

import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.equalTo
import com.natpryce.hamkrest.throws
import org.junit.jupiter.api.Test

abstract class AutoMarshallingJsonContract(marshaller: AutoMarshalling) : AutoMarshallingContract(marshaller) {
    override val expectedAutoMarshallingResult = """{"string":"hello","child":{"string":"world","child":null,"numbers":[1],"bool":true},"numbers":[],"bool":false}"""
    override val expectedAutoMarshallingResultPrimitives = """{"duration":"PT1S","localDate":"2000-01-01","localTime":"01:01:01","localDateTime":"2000-01-01T01:01:01","zonedDateTime":"2000-01-01T01:01:01Z[UTC]","offsetTime":"01:01:01Z","offsetDateTime":"2000-01-01T01:01:01Z","instant":"1970-01-01T00:00:00Z","uuid":"1a448854-1687-4f90-9562-7d527d64383c","uri":"http://uri:8000","url":"http://url:9000","status":200}"""
    override val expectedWrappedMap = """{"value":{"key":"value","key2":"123"}}"""
    override val expectedConvertToInputStream = """{"value":"hello"}"""
    override val expectedThrowable = """{"value":"org.http4k.format.CustomException: foobar"""
    override val inputUnknownValue = """{"value":"value","unknown":"ohno!"}"""
    override val inputEmptyObject = "{}"
    override val expectedRegexSpecial = """{"regex":".*"}"""
    override val expectedAutoMarshallingZonesAndLocale = """{"zoneId":"America/Toronto","zoneOffset":"-04:00","locale":"en-CA"}"""

    val expectedCustomWrappedNumber = """{"value":"1.01"}"""
    val expectedInOutOnly = """{"value":"foobar"}"""
    override val expectedMap = """{"key":"value","key2":"123"}"""
    override val expectedArbitraryArray = """["foo",123.1,{"foo":"bar"},[1.1,2.1],true]"""
    override val expectedArbitrarySet = """["foo","bar"]"""
    override val expectedArbitraryMap = """{"str":"val1","num":123.1,"array":[1.1,"stuff"],"map":{"foo":"bar"},"bool":true}"""

    @Test
    open fun `out only string`() {
        val marshaller = customMarshaller()

        val wrapper = OutOnlyHolder(OutOnly("foobar"))
        val actual = marshaller.asFormatString(wrapper)
        assertThat(actual.normaliseJson(), equalTo(expectedInOutOnly))
        assertThat({ marshaller.asA(actual, OutOnlyHolder::class) }, throws<Exception>())
    }

    @Test
    open fun `in only string`() {
        val marshaller = customMarshaller()

        val wrapper = InOnlyHolder(InOnly("foobar"))
        assertThat({ marshaller.asFormatString(wrapper) }, throws<Exception>())
        assertThat(marshaller.asA(expectedInOutOnly, InOnlyHolder::class), equalTo(wrapper))
    }

    @Test
    open fun `prohibit strings`() {
        val marshaller = customMarshallerProhibitStrings()

        assertThat(marshaller.asFormatString(StringHolder("hello")).normaliseJson(), equalTo(expectedConvertToInputStream))
        assertThat({ marshaller.asA(expectedConvertToInputStream, StringHolder::class) }, throws<Exception>())
    }

    @Test
    open fun `roundtrip custom mapped number`() {
        val marshaller = customMarshaller()

        val wrapper = HolderHolder(MappedBigDecimalHolder(1.01.toBigDecimal()))
        assertThat(marshaller.asFormatString(wrapper).normaliseJson(), equalTo(expectedCustomWrappedNumber))
        assertThat(marshaller.asA(expectedCustomWrappedNumber, HolderHolder::class), equalTo(wrapper))
    }

    @Test
    fun `handles unit`() {
        customMarshaller().asA<Unit>("{}")
    }

}
