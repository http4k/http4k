package org.http4k.format

import com.google.gson.JsonElement
import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.equalTo
import org.http4k.core.Body
import org.http4k.core.Response
import org.http4k.core.Status
import org.http4k.core.with
import org.http4k.format.Gson.auto
import org.junit.Test

class GsonAutoTest : JsonLibAutoMarshallingContract<JsonElement>(Gson) {

    @Test
    fun `roundtrip list of arbitary objects to and from object`() {
        val body = Body.auto<Array<ArbObject>>().toLens()

        val obj = ArbObject("hello", ArbObject("world", null, listOf(1), true), emptyList(), false)

        assertThat(body(Response(Status.OK).with(body of arrayOf(obj))).asList(), equalTo(arrayOf(obj).asList()))
    }
}

class GsonTest : JsonContract<JsonElement, JsonElement>(Gson)
class GsonJsonErrorResponseRendererContractTest : JsonErrorResponseRendererContract<JsonElement, JsonElement>(Gson)
class GsonGenerateDataClassesTest : GenerateDataClassesContract<JsonElement, JsonElement>(Gson)