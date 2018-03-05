package org.http4k.format

import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.equalTo
import org.http4k.core.Body
import org.http4k.core.Response
import org.http4k.core.Status
import org.http4k.core.with
import org.http4k.format.JsonIter.auto
import org.junit.Test

class JsonIterAutoTest : AutoMarshallingContract(JsonIter) {

    @Test
    fun `roundtrip arbitrary object to and from JSON element`() {
        val obj = ArbObject("hello", ArbObject("world", null, listOf(1), true), emptyList(), false)
        val out = JsonIter.asJsonObject(obj)

//        assertThatJson("{\"test\":1}").isEqualTo("{\"test\":2}");


        val actual = JsonIter.asA(out, ArbObject::class)
        assertThat(actual, equalTo(obj))
    }

    @Test
    fun `roundtrip list of arbitrary objects to and from body`() {
        val body = Body.auto<Array<ArbObject>>().toLens()

        val obj = ArbObject("hello", ArbObject("world", null, listOf(1), true), emptyList(), false)

        assertThat(body(Response(Status.OK).with(body of arrayOf(obj))).asList(), equalTo(arrayOf(obj).asList()))
    }

}

class JsonIterTest : JsonContract<JsonAny, JsonAny>(JsonIter) {
    override val prettyString = """{
  "hello": "world"
}"""
}

class JsonIterJsonErrorResponseRendererContractTest : JsonErrorResponseRendererContract<JsonAny, JsonAny>(JsonIter)
class JsonIterGenerateDataClassesTest : GenerateDataClassesContract<JsonAny, JsonAny>(JsonIter)