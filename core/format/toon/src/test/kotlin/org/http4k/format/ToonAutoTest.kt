package org.http4k.format

import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.equalTo
import com.natpryce.hamkrest.startsWith
import dev.toonformat.jtoon.DecodeOptions
import dev.toonformat.jtoon.EncodeOptions
import org.http4k.core.Method.GET
import org.http4k.core.Request
import org.http4k.format.Toon.toon
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test

class ToonAutoTest : AutoMarshallingContract(Toon) {
    override val expectedAutoMarshallingResult = """string:hello
child:
  string:world
  numbers[1]:1
  bool:true
numbers[0]:
bool:false"""
    override val expectedAutoMarshallingResultPrimitives = """period:P1Y2M3D
duration:PT1S
localDate:2000-01-01
localTime:"01:01:01"
localDateTime:"2000-01-01T01:01:01"
zonedDateTime:"2000-01-01T01:01:01Z[UTC]"
offsetTime:"01:01:01Z"
offsetDateTime:"2000-01-01T01:01:01Z"
instant:"1970-01-01T00:00:00Z"
uuid:1a448854-1687-4f90-9562-7d527d64383c
uri:"http://uri:8000"
url:"http://url:9000"
status:200"""
    override val expectedWrappedMap = """value:
  key:value
  key2:"123""""
    override val expectedMap = """key:value
key2:"123""""
    override val expectedArbitraryMap = """str:val1
num:123.1
array[2]:1.1,stuff
map:
  foo:bar
bool:true"""
    override val expectedArbitraryArray = """[5]:
  - foo
  - 123.1
  - foo:bar
  - [2]:1.1,2.1
  - true"""
    override val expectedArbitrarySet = """[2]:foo,bar"""
    override val expectedConvertToInputStream = "value:hello"
    override val expectedThrowable = """"""
    override val inputUnknownValue = """value: value"""
    override val inputEmptyObject = """"""
    override val expectedRegexSpecial = """regex:.*"""
    override val expectedAutoMarshallingZonesAndLocale = """zoneId:America/Toronto
zoneOffset:"-04:00"
locale:en-CA"""

    @Test
    override fun `automarshalling failure has expected message`() {
        assertThat(runCatching { Toon.autoBody<ArbObject>().toLens()(Request(GET, "").body("")) }
            .exceptionOrNull()!!.message!!, startsWith("Required value 'string' missing at \$"))
    }

    @Test
    @Disabled("No support yet")
    override fun `fails decoding when a extra key found`() {
    }

    @Test
    fun `direct injection + extraction into message`() {
        val item = MyValueHolder(MyValue("foobar"))
        val req = Request(GET, "/foo").toon(item)
        assertThat(req.toon<MyValueHolder>(), equalTo(item))
    }

    @Test
    override fun `roundtrip custom value`() {
        val marshaller = customMarshaller()

        val wrapper = MyValueHolder(MyValue("foobar"))
        val expected = """value: foobar"""
        assertThat(marshaller.asFormatString(wrapper), equalTo(expected))
        assertThat(marshaller.asA(expected, MyValueHolder::class), equalTo(wrapper))
        assertThat(marshaller.asA("{\"value\":null}", MyValueHolder::class), equalTo(MyValueHolder(null)))
        assertThat(marshaller.asA("{\"value\":null}", MyValueHolderHolder::class), equalTo(MyValueHolderHolder(null)))
    }

    override fun strictMarshaller() = Toon

    override fun customMarshaller() = ConfigurableToon(
        ToonBuilder().asConfigurable().customise(),
        EncodeOptions.DEFAULT,
        DecodeOptions.DEFAULT
    )

    override fun customMarshallerProhibitStrings() = ConfigurableToon(
        ToonBuilder().asConfigurable().prohibitStrings().customise(), EncodeOptions.DEFAULT,
        DecodeOptions.DEFAULT
    )
}
