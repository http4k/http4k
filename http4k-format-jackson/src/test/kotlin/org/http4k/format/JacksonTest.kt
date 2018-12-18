package org.http4k.format

import com.fasterxml.jackson.databind.JsonNode
import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.equalTo
import org.http4k.core.Body
import org.http4k.core.Response
import org.http4k.core.Status
import org.http4k.core.with
import org.http4k.format.Jackson.auto
import org.http4k.jsonrpc.AutoMappingJsonRpcServiceContract
import org.http4k.jsonrpc.ManualMappingJsonRpcServiceContract
import org.junit.jupiter.api.Test

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
}

class JacksonTest : JsonContract<JsonNode>(Jackson) {
    override val prettyString = """{
  "hello" : "world"
}"""
}

class JacksonJsonErrorResponseRendererTest : JsonErrorResponseRendererContract<JsonNode>(Jackson)
class JacksonGenerateDataClassesTest : GenerateDataClassesContract<JsonNode>(Jackson)
class JacksonAutoMappingJsonRpcServiceTest : AutoMappingJsonRpcServiceContract<JsonNode>(Jackson)
class JacksonManualMappingJsonRpcServiceTest : ManualMappingJsonRpcServiceContract<JsonNode>(Jackson)

