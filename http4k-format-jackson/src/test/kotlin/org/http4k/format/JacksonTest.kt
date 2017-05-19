package org.http4k.format

import com.fasterxml.jackson.databind.JsonNode
import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.equalTo
import org.http4k.core.Body
import org.http4k.core.Response
import org.http4k.core.Status.Companion.OK
import org.http4k.core.with
import org.http4k.format.Jackson.asA
import org.http4k.format.Jackson.asJsonString
import org.http4k.format.Jackson.auto
import org.junit.Test

class JacksonTest : JsonContract<JsonNode, JsonNode>(Jackson) {

    data class ArbObject(val string: String, val child: ArbObject?, val numbers: List<Int>, val bool: Boolean)

    @Test
    fun `roundtrip arbitary object to and from JSON string`() {
        val obj = ArbObject("hello", ArbObject("world", null, listOf(1), true), emptyList(), false)
        val out = obj.asJsonString()
        assertThat(out, equalTo("""{"string":"hello","child":{"string":"world","child":null,"numbers":[1],"bool":true},"numbers":[],"bool":false}"""))
        assertThat(out.asA<ArbObject>(), equalTo(obj))
        assertThat(Jackson.parse(out).asA<ArbObject>(), equalTo(obj))
    }

    @Test
    fun `roundtrip arbitary object to and from object`() {
        val body = Body.auto<ArbObject>()

        val obj = ArbObject("hello", ArbObject("world", null, listOf(1), true), emptyList(), false)

        assertThat(body(Response(OK).with(body of obj)), equalTo(obj))
    }
}

class JacksonJsonErrorResponseRendererTest : JsonErrorResponseRendererContract<JsonNode, JsonNode>(Jackson)

class JacksonGenerateDataClassesTest : GenerateDataClassesContract<JsonNode, JsonNode>(Jackson)