package org.http4k.format

import com.google.gson.GsonBuilder
import com.google.gson.JsonElement
import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.equalTo
import org.http4k.core.Body
import org.http4k.core.Response
import org.http4k.core.Status.Companion.OK
import org.http4k.core.with
import org.http4k.format.Gson.asA
import org.http4k.format.Gson.auto
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test

class GsonAutoTest : AutoMarshallingJsonContract(Gson) {

    @Test
    fun ` roundtrip arbitrary object to and from JSON element`() {
        val obj = ArbObject("hello", ArbObject("world", null, listOf(1), true), emptyList(), false)
        val out = Gson.asJsonObject(obj)
        assertThat(asA(out, ArbObject::class), equalTo(obj))
    }

    @Test
    fun `roundtrip list of arbitrary objects to and from node`() {
        val obj = ArbObject("hello", ArbObject("world", null, listOf(1), true), emptyList(), false)

        assertThat(Gson.asJsonObject(listOf(obj)).asA(), equalTo(listOf(obj)))
    }

    @Test
    fun `roundtrip list of arbitrary objects to and from body`() {
        val body = Body.auto<List<ArbObject>>().toLens()

        val obj = ArbObject("hello", ArbObject("world", null, listOf(1), true), emptyList(), false)

        assertThat(body(Response(OK).with(body of listOf(obj))), equalTo(listOf(obj)))
    }

    @Test
    fun `roundtrip array of arbitrary objects to and from body`() {
        val body = Body.auto<Array<ArbObject>>().toLens()

        val obj = ArbObject("hello", ArbObject("world", null, listOf(1), true), emptyList(), false)

        assertThat(body(Response(OK).with(body of arrayOf(obj))).toList(), equalTo(listOf(obj)))
    }

    @Test
    @Disabled("GSON does not currently have Kotlin class support")
    override fun `fails decoding when a required value is null`() {
    }

    override fun customMarshaller() = object : ConfigurableGson(GsonBuilder().asConfigurable().customise()) {}
    override fun customMarshallerProhibitStrings() = object : ConfigurableGson(GsonBuilder().asConfigurable().prohibitStrings()
        .customise()) {}
}

class GsonTest : JsonContract<JsonElement>(Gson) {
    override val prettyString = """{
  "hello": "world"
}"""
}

class GsonAutoEventsTest : AutoMarshallingEventsContract(Gson)
