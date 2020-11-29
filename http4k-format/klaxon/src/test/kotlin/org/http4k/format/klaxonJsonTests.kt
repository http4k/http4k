package org.http4k.format

import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.equalTo
import org.http4k.core.Body
import org.http4k.core.Response
import org.http4k.core.Status.Companion.OK
import org.http4k.core.with
import org.http4k.format.Klaxon.auto
import org.junit.jupiter.api.Test

class KlaxonAutoTest : AutoMarshallingJsonContract(Klaxon) {
    override val expectedAutoMarshallingResult = """{"bool":false, "child":{"bool":true, "child":null, "numbers":[1], "string":"world"}, "numbers":[], "string":"hello"}"""

    override val expectedAutoMarshallingResultPrimitives = """{"duration":"PT1S", "instant":"1970-01-01T00:00:00Z", "localDate":"2000-01-01", "localDateTime":"2000-01-01T01:01:01", "localTime":"01:01:01", "offsetDateTime":"2000-01-01T01:01:01Z", "offsetTime":"01:01:01Z", "status":200, "uri":"http://uri:8000", "url":"http://url:9000", "uuid":"1a448854-1687-4f90-9562-7d527d64383c", "zonedDateTime":"2000-01-01T01:01:01Z[UTC]"}"""

    override fun customMarshaller() = object : ConfigurableKlaxon(com.beust.klaxon.Klaxon().asConfigurable().customise()) {}

    @Test
    fun `roundtrip list of arbitary objects to and from body`() {
        val body = Body.auto<List<ArbObject>>().toLens()

        val obj = ArbObject("hello", ArbObject("world", null, listOf(1), true), emptyList(), false)

        assertThat(body(Response(OK).with(body of listOf(obj))), equalTo(listOf(obj)))
    }

    @Test
    fun `roundtrip array of arbitary objects to and from body`() {
        val body = Body.auto<Array<ArbObject>>().toLens()

        val obj = ArbObject("hello", ArbObject("world", null, listOf(1), true), emptyList(), false)

        assertThat(body(Response(OK).with(body of arrayOf(obj))).toList(), equalTo(listOf(obj)))
    }

    @Test
    fun `roundtrip polymorphic object to and from body`() {
        val body = Body.auto<PolymorphicParent>().toLens()

        val firstChild: PolymorphicParent = FirstChild("hello")
        val secondChild: PolymorphicParent = SecondChild("world")

        assertThat(body(Response(OK).with(body of firstChild)), equalTo(firstChild))
        assertThat(body(Response(OK).with(body of secondChild)), equalTo(secondChild))
    }

    @Test
    fun `write interface implementation to body`() {
        assertThat(Response(OK).with(
            Body.auto<Interface>().toLens() of InterfaceImpl()
        ).bodyString(), equalTo("""{"subValue":"123", "value":"hello"}"""))
    }

    @Test
    fun `write list of interface implementation to body`() {
        assertThat(Response(OK).with(
            Body.auto<List<Interface>>().toLens() of listOf(InterfaceImpl())
        ).bodyString(), equalTo("""[{"subValue":"123", "value":"hello"}]"""))
    }

    @Test
    fun `writes using non-sealed parent type`() {
        val nonSealedChild = NonSealedChild("hello")
        assertThat(Response(OK).with(Body.auto<NotSealedParent>().toLens() of nonSealedChild).bodyString(), equalTo("""{"something":"hello"}"""))
    }

    @Test
    fun `roundtrip list of polymorphic objects to and from body`() {
        val body = Body.auto<List<PolymorphicParent>>().toLens()

        val list = listOf(FirstChild("hello"), SecondChild("world"))

        assertThat(body(Response(OK).with(body of list)), equalTo(list))
    }
}

class KlaxonAutoEventsTest : AutoJsonEventsContract(Klaxon)
