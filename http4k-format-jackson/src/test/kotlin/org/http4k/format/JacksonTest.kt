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
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZonedDateTime
import java.util.UUID

class JacksonAutoTest : AutoMarshallingContract(Jackson) {

    @Test
    fun ` roundtrip arbitary object to and from JSON element`() {
        val obj = ArbObject("hello", ArbObject("world", null, listOf(1), true), emptyList(), false)
        val out = Jackson.asJsonObject(obj)
        assertThat(Jackson.asA(out, ArbObject::class), equalTo(obj))
    }

    @Test
    fun `roundtrip list of arbitary objects to and from body`() {
        val body = Body.auto<Array<ArbObject>>().toLens()

        val obj = ArbObject("hello", ArbObject("world", null, listOf(1), true), emptyList(), false)

        assertThat(body(Response(Status.OK).with(body of arrayOf(obj))).toList(), equalTo(arrayOf(obj).toList()))
    }

    @Test
    fun `roundtrip object with common java primitive types`() {
        val body = Body.auto<CommonJdkPrimitives>().toLens()

        val obj = CommonJdkPrimitives(LocalDate.now(), LocalDateTime.now(), ZonedDateTime.now(), UUID.randomUUID())

        assertThat(body(Response(Status.OK).with(body of obj)), equalTo(obj))
    }
}

class JacksonTest : JsonContract<JsonNode, JsonNode>(Jackson) {
    override val prettyString = """{
  "hello" : "world"
}"""
}
class JacksonJsonErrorResponseRendererTest : JsonErrorResponseRendererContract<JsonNode, JsonNode>(Jackson)
class JacksonGenerateDataClassesTest : GenerateDataClassesContract<JsonNode, JsonNode>(Jackson)