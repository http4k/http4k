package org.http4k.format

import com.beust.klaxon.JsonObject
import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.equalTo
import org.http4k.core.Body
import org.http4k.core.Response
import org.http4k.core.Status.Companion.OK
import org.http4k.core.with
import org.http4k.format.Klaxon.asA
import org.http4k.format.Klaxon.auto
import org.junit.jupiter.api.Test

class KlaxonAutoTest : AutoMarshallingJsonContract(Klaxon) {

    @Test
    fun `roundtrip arbitary object to and from JSON element`() {
        val obj = ArbObject("hello", ArbObject("world", null, listOf(1), true), emptyList(), false)
        val out = Klaxon.asJsonObject(obj)
        assertThat(Klaxon.asA(out, ArbObject::class), equalTo(obj))
    }

    @Test
    fun `roundtrip list of arbitary objects to and from node`() {
        val obj = ArbObject("hello", ArbObject("world", null, listOf(1), true), emptyList(), false)

        assertThat(Klaxon.asJsonObject(listOf(obj)).asA(), equalTo(listOf(obj)))
    }

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
        ).bodyString(), equalTo("""{"value":"hello","subValue":"123"}"""))
    }

    @Test
    fun `write list of interface implementation to body`() {
        assertThat(Response(OK).with(
            Body.auto<List<Interface>>().toLens() of listOf(InterfaceImpl())
        ).bodyString(), equalTo("""[{"value":"hello","subValue":"123"}]"""))
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

class KlaxonTest : JsonContract<JsonObject>(Klaxon) {
    override val prettyString = """{
  "hello" : "world"
}"""
}

class KlaxonAutoEventsTest : AutoJsonEventsContract(Klaxon)
