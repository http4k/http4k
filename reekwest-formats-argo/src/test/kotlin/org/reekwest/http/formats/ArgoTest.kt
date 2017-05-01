package org.reekwest.http.formats

import com.natpryce.hamkrest.anything
import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.equalTo
import com.natpryce.hamkrest.throws
import org.junit.Test
import org.reekwest.http.core.Request.Companion.get
import org.reekwest.http.core.with
import org.reekwest.http.formats.Argo.asCompactJsonString
import org.reekwest.http.formats.Argo.asJsonArray
import org.reekwest.http.formats.Argo.asJsonObject
import org.reekwest.http.formats.Argo.asJsonValue
import org.reekwest.http.formats.Argo.fromJsonString
import org.reekwest.http.formats.Argo.json
import org.reekwest.http.lens.BiDiLensContract.checkContract
import org.reekwest.http.lens.BiDiLensContract.spec
import org.reekwest.http.lens.Body
import java.math.BigDecimal
import java.math.BigInteger

class ArgoTest {

    @Test
    fun `serializes object to json`() {
        val nullable: String? = null
        val input = listOf(
            "string" to "value".asJsonValue(),
            "double" to 1.0.asJsonValue(),
            "long" to 10L.asJsonValue(),
            "boolean" to true.asJsonValue(),
            "bigDec" to BigDecimal(1.2).asJsonValue(),
            "bigInt" to BigInteger("12344").asJsonValue(),
            "null" to nullable.asJsonValue(),
            "int" to 2.asJsonValue(),
            "array" to listOf(
                "".asJsonValue(),
                123.asJsonValue()
            ).asJsonArray()
        ).asJsonObject()
        val expected = """{"string":"value","double":1,"long":10,"boolean":true,"bigDec":1.1999999999999999555910790149937383830547332763671875,"bigInt":12344,"null":null,"int":2,"array":["",123]}"""
        assertThat(input.asCompactJsonString(), equalTo(expected))
    }

    @Test
    fun `can write and read body as Json`() {
        val body = Body.json().required()

        val obj = listOf("hello" to "world".asJsonValue()).asJsonObject()

        val request = get("/bob")

        val requestWithBody = request.with(body to obj)

        assertThat(requestWithBody.bodyString(), equalTo("""{"hello":"world"}"""))

        assertThat(body(requestWithBody), equalTo(obj))
    }

    @Test
    fun `can write and read spec as Json`() {
        checkContract(spec.json(), """{"hello":"world"}""", Argo.obj("hello" to "world".asJsonValue()))
    }

    @Test
    fun `invalid Json blows up parse`() {
        assertThat({ "".fromJsonString() }, throws(anything))
    }

}