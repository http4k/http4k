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

class JacksonAutoTest : JsonLibAutoMarshallingContract<JsonNode>(Jackson) {
    @Test
    fun `roundtrip list of arbitary objects to and from object`() {
        val body = Body.auto<Array<ArbObject>>().toLens()

        val obj = ArbObject("hello", ArbObject("world", null, listOf(1), true), emptyList(), false)

        assertThat(body(Response(Status.OK).with(body of arrayOf(obj))).toList(), equalTo(arrayOf(obj).toList()))
    }
}

class JacksonTest : JsonContract<JsonNode, JsonNode>(Jackson)
class JacksonJsonErrorResponseRendererTest : JsonErrorResponseRendererContract<JsonNode, JsonNode>(Jackson)
class JacksonGenerateDataClassesTest : GenerateDataClassesContract<JsonNode, JsonNode>(Jackson)