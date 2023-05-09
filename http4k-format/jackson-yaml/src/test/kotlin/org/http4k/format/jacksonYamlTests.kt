package org.http4k.format

import com.fasterxml.jackson.module.kotlin.KotlinModule
import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.equalTo
import org.http4k.core.Body
import org.http4k.core.Response
import org.http4k.core.Status
import org.http4k.core.with
import org.http4k.format.JacksonYaml.auto
import org.http4k.lens.BiDiMapping
import org.junit.jupiter.api.Test
import java.math.BigDecimal

class JacksonYamlBodyTest {

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
    fun `write interface implementation to body`() {
        assertThat(
            Response(Status.OK).with(
                Body.auto<Interface>().toLens() of InterfaceImpl()
            ).bodyString(), equalTo(
                """value: "hello"
subValue: "123"
"""
            )
        )
    }

    @Test
    fun `write list of interface implementation to body`() {
        assertThat(
            Response(Status.OK).with(
                Body.auto<List<Interface>>().toLens() of listOf(InterfaceImpl())
            ).bodyString(), equalTo(
                """- value: "hello"
  subValue: "123"
"""
            )
        )
    }

    @Test
    fun `writes using non-sealed parent type`() {
        val nonSealedChild = NonSealedChild("hello")
        assertThat(
            Response(Status.OK).with(Body.auto<NotSealedParent>().toLens() of nonSealedChild).bodyString(), equalTo(
                """something: "hello"
"""
            )
        )
    }

    @Test
    fun `roundtrip list of polymorphic objects to and from body`() {
        val body = Body.auto<List<PolymorphicParent>>().toLens()

        val list = listOf(FirstChild("hello"), SecondChild("world"))

        assertThat(body(Response(Status.OK).with(body of list)), equalTo(list))
    }
}

class JacksonYamlAutoTest : AutoMarshallingContract(JacksonYaml) {
    override val expectedAutoMarshallingResult: String = """string:"hello"
child:
  string:"world"
  child: null
  numbers:
  - 1
  bool: true
numbers: []
bool: false
"""
    override val expectedAutoMarshallingResultPrimitives: String = """duration: "PT1S"
localDate: "2000-01-01"
localTime: "01:01:01"
localDateTime: "2000-01-01T01:01:01"
zonedDateTime: "2000-01-01T01:01:01Z[UTC]"
offsetTime: "01:01:01Z"
offsetDateTime: "2000-01-01T01:01:01Z"
instant: "1970-01-01T00:00:00Z"
uuid: "1a448854-1687-4f90-9562-7d527d64383c"
uri: "http://uri:8000"
url: "http://url:9000"
status: 200
"""
    override val expectedWrappedMap: String = """value:
  key: "value"
  key2: "123"
"""

    override val expectedConvertToInputStream: String = """value: "hello"
"""
    override val expectedThrowable: String = """value: "org.http4k.format.CustomException: foobar"""
    override val inputUnknownValue: String = """value: "value"
unknown: "2000-01-01"        
"""
    override val inputEmptyObject: String = """"""
    override val expectedRegexSpecial: String = """regex: ".*"
"""
    override val expectedMap = """key:"value"
key2:"123"
"""

    override val expectedAbitraryArray = """- "foo"
- 123.1
- foo:"bar"
- - 1.1
  - 2.1
- true
"""

    override val expectedArbitraryMap = """str: "val1"
num: 123.1
array:
- 1.1
- "stuff"
map:
  foo: "bar"
bool: true
"""

    override val expectedAutoMarshallingZonesAndLocale = "zoneId:\"America/Toronto\"\nzoneOffset:\"-04:00\"\nlocale:\"en-CA\"\n"

    @Test
    override fun `roundtrip arbitrary map`() {
        val wrapper = mapOf(
            "str" to "val1",
            "num" to BigDecimal("123.1"),
            "array" to listOf(BigDecimal("1.1"),"stuff"),
            "map" to mapOf("foo" to "bar"),
            "bool" to true
        )
        val asString = JacksonYaml.asFormatString(wrapper)
        assertThat(asString.normaliseJson(), equalTo(expectedArbitraryMap.normaliseJson()))
        assertThat(JacksonYaml.asA(asString), equalTo(wrapper))
    }

    @Test
    override fun `roundtrip arbitrary array`() {
        val wrapper = listOf(
            "foo",
            BigDecimal("123.1"),
            mapOf("foo" to "bar"),
            listOf(BigDecimal("1.1"), BigDecimal("2.1")),
            true
        )
        val asString = JacksonYaml.asFormatString(wrapper)
        assertThat(asString.normaliseJson(), equalTo(expectedAbitraryArray.normaliseJson()))
        assertThat(JacksonYaml.asA(asString), equalTo(wrapper))
    }

    @Test
    fun `custom jackson yaml`() {
        val jackson = JacksonYaml.custom {
            text(BiDiMapping({StringHolder(it)},{it.value}))
        }

        val value = StringHolder("stuff")
        assertThat(jackson.asFormatString(value), equalTo("\"stuff\"\n"))
    }

    override fun strictMarshaller() =
        object : ConfigurableJacksonYaml(KotlinModule.Builder().build().asConfigurable().customise()) {}

    override fun customMarshaller() =
        object : ConfigurableJacksonYaml(KotlinModule.Builder().build().asConfigurable().customise()) {}

    override fun customMarshallerProhibitStrings() =
        object : ConfigurableJacksonYaml(KotlinModule.Builder().build().asConfigurable().customise()) {}
}
