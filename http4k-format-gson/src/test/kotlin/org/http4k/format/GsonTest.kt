package org.http4k.format

import com.google.gson.GsonBuilder
import com.google.gson.JsonElement
import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.equalTo
import org.http4k.core.Body
import org.http4k.core.Response
import org.http4k.core.Status
import org.http4k.core.with
import org.http4k.format.Gson.auto
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test

class GsonAutoTest : AutoMarshallingContract(Gson) {

    @Test
    fun ` roundtrip arbitary object to and from JSON element`() {
        val obj = ArbObject("hello", ArbObject("world", null, listOf(1), true), emptyList(), false)
        val out = Gson.asJsonObject(obj)
        assertThat(Gson.asA(out, ArbObject::class), equalTo(obj))
    }

    @Test
    fun `roundtrip list of arbitary objects to and from body`() {
        val body = Body.auto<Array<ArbObject>>().toLens()

        val obj = ArbObject("hello", ArbObject("world", null, listOf(1), true), emptyList(), false)

        assertThat(body(Response(Status.OK).with(body of arrayOf(obj))).asList(), equalTo(arrayOf(obj).asList()))
    }

    @Test
    @Disabled("GSON does not currently have Kotlin class support")
    override fun `fails decoding when a required value is null`() {
    }

    override fun customJson() = object : ConfigurableGson(GsonBuilder().asConfigurable().customise()) {}
}

class GsonTest : JsonContract<JsonElement>(Gson) {
    override val prettyString = """{
  "hello": "world"
}"""
}

class GsonGenerateDataClassesTest : GenerateDataClassesContract<JsonElement>(Gson)

class GsonAutoEventsTest : AutoJsonEventsContract(Gson)