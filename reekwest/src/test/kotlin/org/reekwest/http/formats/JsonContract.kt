package org.reekwest.http.formats

import com.natpryce.hamkrest.anything
import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.equalTo
import com.natpryce.hamkrest.throws
import org.junit.Test
import org.reekwest.http.core.Request
import org.reekwest.http.core.with
import org.reekwest.http.lens.BiDiLensContract.checkContract
import org.reekwest.http.lens.BiDiLensContract.spec
import java.math.BigDecimal
import java.math.BigInteger

abstract class JsonContract<ROOT : NODE, NODE>(val j: Json<ROOT, NODE>) {

    @Test
    fun `looks up types`() {
        assertThat(j.typeOf(j.string("")), equalTo(JsonType.String))
        assertThat(j.typeOf(j.number(1)), equalTo(JsonType.Number))
        assertThat(j.typeOf(j.boolean(true)), equalTo(JsonType.Boolean))
        assertThat(j.typeOf(j.nullNode()), equalTo(JsonType.Null))
        assertThat(j.typeOf(j.obj("name" to j.string(""))), equalTo(JsonType.Object))
        assertThat(j.typeOf(j.array(listOf(j.string("")))), equalTo(JsonType.Array))
    }

    @Test
    fun `serializes object to json`() {
        val input = j.obj(listOf(
            "string" to j.string("value"),
            "double" to j.number(1.0),
            "long" to j.number(10L),
            "boolean" to j.boolean(true),
            "bigDec" to j.number(BigDecimal(1.2)),
            "bigInt" to j.number(BigInteger("12344")),
            "null" to j.nullNode(),
            "int" to j.number(2),
            "array" to j.array(listOf(
                j.string(""),
                j.number(123)
            ))
        ))
        val expected = """{"string":"value","double":1,"long":10,"boolean":true,"bigDec":1.1999999999999999555910790149937383830547332763671875,"bigInt":12344,"null":null,"int":2,"array":["",123]}"""
        assertThat(j.compact(input), equalTo(expected))
    }

    @Test
    fun `can write and read body as json`() {
        val body = j.body().required()

        val obj = j.obj(listOf("hello" to j.string("world")))

        val request = Request.get("/bob")

        val requestWithBody = request.with(body to obj)

        assertThat(requestWithBody.bodyString(), equalTo("""{"hello":"world"}"""))

        assertThat(body(requestWithBody), equalTo(obj))
    }

    @Test
    fun `get fields`() {
        val fields = listOf("hello" to j.string("world"), "hello2" to j.string("world2"))
        assertThat(j.fields(j.obj(fields)).toList(), equalTo(fields))
    }

    @Test
    fun `get elements`() {
        val fields = listOf(j.string("world"), j.string("world2"))
        val elements = j.elements(j.array(fields)).toList()
        assertThat(elements, equalTo(fields))
    }

    @Test
    fun `can write and read spec as json`() {
        checkContract(j.lens(spec), """{"hello":"world"}""", j.obj("hello" to j.string("world")))
    }

    @Test
    fun `invalid json blows up parse`() {
        assertThat({ j.parse("") }, throws(anything))
    }
}