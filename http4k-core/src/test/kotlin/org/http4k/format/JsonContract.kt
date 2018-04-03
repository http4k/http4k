package org.http4k.format

import com.natpryce.hamkrest.anything
import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.equalTo
import com.natpryce.hamkrest.throws
import org.http4k.core.Method
import org.http4k.core.Request
import org.http4k.core.with
import org.http4k.lens.BiDiLensContract.checkContract
import org.http4k.lens.BiDiLensContract.spec
import org.junit.Test
import java.math.BigDecimal
import java.math.BigInteger

abstract class JsonContract<ROOT : NODE, NODE>(open val j: Json<ROOT, NODE>) {

    abstract val prettyString: String

    @Test
    fun `looks up types`() {
        assertThat(j.typeOf(j.string("")), equalTo(JsonType.String))
        assertThat(j.typeOf(j.number(1)), equalTo(JsonType.Number))
        assertThat(j.typeOf(j.number(1.0)), equalTo(JsonType.Number))
        assertThat(j.typeOf(j.boolean(true)), equalTo(JsonType.Boolean))
        assertThat(j.typeOf(j.nullNode()), equalTo(JsonType.Null))
        assertThat(j.typeOf(j.obj("name" to j.string(""))), equalTo(JsonType.Object))
        assertThat(j.typeOf(j.array(listOf(j.string("")))), equalTo(JsonType.Array))
    }

    @Test
    fun `serializes object to json`() {
        val input = j.obj(
            "string" to j.string("value"),
            "double" to j.number(1.5),
            "long" to j.number(10L),
            "boolean" to j.boolean(true),
            "bigDec" to j.number(BigDecimal(1.2)),
            "bigInt" to j.number(BigInteger("12344")),
            "null" to j.nullNode(),
            "int" to j.number(2),
            "empty" to j.obj(),
            "array" to j.array(listOf(
                j.string(""),
                j.number(123)
            ))
        )
        val expected = """{"string":"value","double":1.5,"long":10,"boolean":true,"bigDec":1.1999999999999999555910790149937383830547332763671875,"bigInt":12344,"null":null,"int":2,"empty":{},"array":["",123]}"""
        assertThat(j.compact(input), equalTo(expected))
    }

    @Test
    fun `can write and read body as json`() {
        val body = j.body().toLens()

        val obj = j.obj("hello" to j.string("world"))

        val request = Request(Method.GET, "/bob")

        val requestWithBody = request.with(body of obj)

        assertThat(requestWithBody.bodyString(), equalTo("""{"hello":"world"}"""))

        assertThat(body(requestWithBody), equalTo(obj))
    }

    @Test
    fun `get fields`() {
        val fields = listOf("hello" to j.string("world"), "hello2" to j.string("world2"))
        assertThat(j.fields(j.obj(*fields.toTypedArray())).toList(), equalTo(fields))
    }

    @Test
    fun `get text`() {
        assertThat(j.text(j.string("world")), equalTo("world"))
    }

    @Test
    fun `get elements`() {
        val fields = listOf(j.string("world"), j.string("world2"))
        val elements = j.elements(j.array(fields)).toList()
        assertThat(elements, equalTo(fields))
    }

    @Test
    fun `can write and read spec as json`() {
        val validValue = """{"hello":"world"}"""
        checkContract(j.lens(spec), j.obj("hello" to j.string("world")), validValue, "", "hello", "o", "o$validValue", "o$validValue$validValue")
    }

    @Test
    fun `invalid json blows up parse`() {
        assertThat({ j.parse("") }, throws(anything))
        assertThat({ j.parse("somevalue") }, throws(anything))
    }

    @Test
    fun `compactify`() {
        assertThat(j.compactify("""{   "hello"  :  "world"   }"""), equalTo("""{"hello":"world"}"""))
    }

    @Test
    fun `prettify`() {
        assertThat(j.prettify("""{"hello":"world"}"""), equalTo(prettyString))
    }
}