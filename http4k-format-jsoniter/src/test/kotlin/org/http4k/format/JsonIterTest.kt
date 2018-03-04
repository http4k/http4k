package org.http4k.format

import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.equalTo
import org.http4k.core.Body
import org.http4k.core.Response
import org.http4k.core.Status
import org.http4k.core.with
import import org.http4k.format.JsonIter.auto
import org.junit.Ignore
import org.junit.Test

class JsonIterAutoTest : AutoMarshallingContract(JsonIter) {

    @Test
    fun `roundtrip arbitary object to and from JSON element`() {
        val obj = ArbObject("hello", ArbObject("world", null, listOf(1), true), emptyList(), false)
        val out = JsonIter.asJsonObject(obj)
        assertThat(JsonIter.asA(out, ArbObject::class), equalTo(obj))
    }

    @Test
    fun `roundtrip list of arbitary objects to and from body`() {
        val body = Body.auto<Array<ArbObject>>().toLens()

        val obj = ArbObject("hello", ArbObject("world", null, listOf(1), true), emptyList(), false)

        assertThat(body(Response(Status.OK).with(body of arrayOf(obj))).asList(), equalTo(arrayOf(obj).asList()))
    }

    @Test
    @Ignore("JsonIter does not currently have Kotlin class support") // TODO Is that true?
    override fun `fails decoding when a required value is null`() {
    }

}

class JsonIterTest : JsonContract<JsonAny, JsonAny>(JsonIter) {
    override val prettyString = """{
  "hello": "world"
}"""
}

class JsonIterJsonErrorResponseRendererContractTest : JsonErrorResponseRendererContract<JsonAny, JsonAny>(JsonIter)
class JsonIterGenerateDataClassesTest : GenerateDataClassesContract<JsonAny, JsonAny>(JsonIter)