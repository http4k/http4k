package org.http4k.format

import com.fasterxml.jackson.databind.JsonNode
import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.equalTo
import org.http4k.core.Body
import org.http4k.format.Jackson.asA
import org.http4k.format.Jackson.asJsonNode
import org.http4k.format.Jackson.asJsonString
import org.http4k.format.Jackson.json
import org.http4k.lens.BiDiBodyLensSpec
import org.junit.Test
import java.nio.ByteBuffer

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
    fun `custom body is roundtrippable`() {
        val obj = ArbObject("hello", ArbObject("world", null, listOf(1), true), emptyList(), false)
        val out = obj.asJsonString()
        val map: BiDiBodyLensSpec<ByteBuffer, ArbObject> = biDiBodyLensSpec()
        map to obj
        assertThat(out, equalTo("""{"string":"hello","child":{"string":"world","child":null,"numbers":[1],"bool":true},"numbers":[],"bool":false}"""))
        assertThat(out.asA<ArbObject>(), equalTo(obj))
        assertThat(Jackson.parse(out).asA<ArbObject>(), equalTo(obj))
    }

    private inline fun <reified T : Any> biDiBodyLensSpec() = Body.json().map({ json -> json.asA<T>() }, { it.asJsonNode() })
}

class JacksonJsonErrorResponseRendererContractTest : JsonErrorResponseRendererContract<JsonNode, JsonNode>(Jackson)