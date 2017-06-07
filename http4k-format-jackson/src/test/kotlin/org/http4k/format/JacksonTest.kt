package org.http4k.format

import com.fasterxml.jackson.databind.JsonNode
import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.equalTo
import org.http4k.core.Body
import org.http4k.core.Response
import org.http4k.core.Status
import org.http4k.core.with
import org.http4k.format.Jackson.auto
import org.junit.Test

//class JacksonAutoTest : AutoMarshallingContract<JsonNode>(Jackson)

class JacksonTest : JsonContract<JsonNode, JsonNode>(Jackson) {

    override val j = Jackson

    data class ArbObject(val string: String, val child: ArbObject?, val numbers: List<Int>, val bool: Boolean)

    @Test
    fun `roundtrip arbitary object to and from JSON string`() {
        val obj = ArbObject("hello", ArbObject("world", null, listOf(1), true), emptyList(), false)
        val out = j.compact(j.asJsonObject(obj))
        assertThat(out, equalTo("""{"string":"hello","child":{"string":"world","child":null,"numbers":[1],"bool":true},"numbers":[],"bool":false}"""))
        assertThat(j.asA(out, ArbObject::class), equalTo(obj))
        assertThat(j.asA(j.parse(out), ArbObject::class), equalTo(obj))
    }

    @Test
    fun `roundtrip arbitary object to and from object`() {
        val body = Body.auto<ArbObject>().toLens()

        val obj = ArbObject("hello", ArbObject("world", null, listOf(1), true), emptyList(), false)

        assertThat(body(Response(Status.OK).with(body of obj)), equalTo(obj))
    }

}

class JacksonJsonErrorResponseRendererTest : JsonErrorResponseRendererContract<JsonNode, JsonNode>(Jackson)

class JacksonGenerateDataClassesTest : GenerateDataClassesContract<JsonNode, JsonNode>(Jackson)