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

class JacksonAutoTest : AutoMarshallingContract<JsonNode>(Jackson)

class JacksonTest : JsonContract<JsonNode, JsonNode>(Jackson) {

    @Test
    fun `roundtrip arbitary object to and from object`() {
        val body = Body.auto<ArbObject>().toLens()

        val obj = ArbObject("hello", ArbObject("world", null, listOf(1), true), emptyList(), false)

        assertThat(body(Response(Status.OK).with(body of obj)), equalTo(obj))
    }

}

class JacksonJsonErrorResponseRendererTest : JsonErrorResponseRendererContract<JsonNode, JsonNode>(Jackson)

class JacksonGenerateDataClassesTest : GenerateDataClassesContract<JsonNode, JsonNode>(Jackson)