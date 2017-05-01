package org.reekwest.http.formats

import com.natpryce.hamkrest.anything
import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.equalTo
import com.natpryce.hamkrest.throws
import org.junit.Test
import org.reekwest.http.core.Request.Companion.get
import org.reekwest.http.core.with
import org.reekwest.http.formats.Jackson.asCompactJsonString
import org.reekwest.http.formats.Jackson.asJson
import org.reekwest.http.formats.Jackson.asJsonArray
import org.reekwest.http.formats.Jackson.asJsonObject
import org.reekwest.http.formats.Jackson.fromJsonString
import org.reekwest.http.formats.Jackson.json
import org.reekwest.http.lens.BiDiLensContract.checkContract
import org.reekwest.http.lens.BiDiLensContract.spec
import org.reekwest.http.lens.Body
import java.math.BigDecimal
import java.math.BigInteger

class JacksonTest {

    @Test
    fun `serializes object to json`() {
        val nullable: String? = null
        val input = listOf(
            "string" to "value".asJson(),
            "double" to 1.0.asJson(),
            "long" to 10L.asJson(),
            "boolean" to true.asJson(),
            "bigDec" to BigDecimal(1.2).asJson(),
            "bigInt" to BigInteger("12344").asJson(),
            "null" to nullable.asJson(),
            "int" to 2.asJson(),
            "array" to listOf(
                "".asJson(),
                123.asJson()
            ).asJsonArray()
        ).asJsonObject()
        val expected = """{"string":"value","double":1,"long":10,"boolean":true,"bigDec":1.1999999999999999555910790149937383830547332763671875,"bigInt":12344,"null":null,"int":2,"array":["",123]}"""
        assertThat(input.asCompactJsonString(), equalTo(expected))
    }

    @Test
    fun `can write and read body as Json`() {
        val body = Body.json().required()

        val obj = listOf("hello" to "world".asJson()).asJsonObject()

        val request = get("/bob")

        val requestWithBody = request.with(body to obj)

        assertThat(requestWithBody.bodyString(), equalTo("""{"hello":"world"}"""))

        assertThat(body(requestWithBody), equalTo(obj))
    }

    @Test
    fun `can write and read query as Json`() {
        checkContract(spec.json(), """{"hello":"world"}""", Jackson.obj("hello" to "world".asJson()))
    }

    @Test
    fun `invalid Json blows up parse`() {
        assertThat({ "".fromJsonString() }, throws(anything))
    }

}