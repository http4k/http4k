package org.http4k.format

import com.fasterxml.jackson.databind.JsonNode
import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.equalTo
import org.http4k.format.Jackson.asA
import org.http4k.format.Jackson.asJsonString
import org.http4k.formats.JsonContract
import org.http4k.formats.JsonErrorResponseRendererContract
import org.junit.Test

class JacksonTest : JsonContract<JsonNode, JsonNode>(Jackson) {

    data class ArbObject(val string: String, val child: ArbObject?, val numbers: List<Int>, val bool: Boolean)

    @Test
    fun `roundtrip arbitary object to and from JSON string`() {
        val obj = ArbObject("hello", ArbObject("world", null, listOf(1), true), emptyList(), false)
        val out = obj.asJsonString()
        assertThat(out, equalTo("""{"string":"hello","child":{"string":"world","child":null,"numbers":[1],"bool":true},"numbers":[],"bool":false}"""))
        assertThat(out.asA<ArbObject>(), equalTo(obj))
    }
}

class JacksonJsonErrorResponseRendererContractTest : JsonErrorResponseRendererContract<JsonNode, JsonNode>(Jackson)