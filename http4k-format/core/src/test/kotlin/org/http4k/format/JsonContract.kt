package org.http4k.format

import com.natpryce.hamkrest.anything
import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.equalTo
import com.natpryce.hamkrest.throws
import org.http4k.core.Method.GET
import org.http4k.core.Request
import org.http4k.core.with
import org.http4k.lens.BiDiLensContract.checkContract
import org.http4k.lens.BiDiLensContract.spec
import org.junit.jupiter.api.Test
import java.math.BigDecimal
import java.math.BigInteger

abstract class JsonContract<NODE>(open val j: Json<NODE>) {

    abstract val prettyString: String

    @Test
    fun `looks up types`() {
        j {
            assertThat(typeOf(string("")), equalTo(JsonType.String))
            assertThat(typeOf(number(1)), equalTo(JsonType.Integer))
            assertThat(typeOf(number(1.1)), equalTo(JsonType.Number))
            assertThat(typeOf(boolean(true)), equalTo(JsonType.Boolean))
            assertThat(typeOf(nullNode()), equalTo(JsonType.Null))
            assertThat(typeOf(obj("name" to string(""))), equalTo(JsonType.Object))
            assertThat(typeOf(array(listOf(string("")))), equalTo(JsonType.Array))
        }
    }

    @Test
    open fun `serializes object to json`() {
        j {
            val input = obj(
                "string" to string("value"),
                "double" to number(1.5),
                "long" to number(10L),
                "boolean" to boolean(true),
                "bigDec" to number(BigDecimal(1.2)),
                "bigInt" to number(BigInteger("12344")),
                "null" to nullNode(),
                "int" to number(2),
                "empty" to obj(),
                "array" to array(listOf(
                    string(""),
                    number(123)
                )),
                "singletonArray" to array(obj("number" to number(123)))
            )
            val expected = """{"string":"value","double":1.5,"long":10,"boolean":true,"bigDec":1.1999999999999999555910790149937383830547332763671875,"bigInt":12344,"null":null,"int":2,"empty":{},"array":["",123],"singletonArray":[{"number":123}]}"""
            assertThat(compact(input), equalTo(expected))
        }
    }

    @Test
    fun `can write and read body as json`() {
        j {
            val body = body().toLens()

            val obj = obj("hello" to string("world"))

            val request = Request(GET, "/bob")

            val requestWithBody = request.with(body of obj)

            assertThat(requestWithBody.bodyString(), equalTo("""{"hello":"world"}"""))

            assertThat(body(requestWithBody), equalTo(obj))
        }
    }

    @Test
    fun `get fields`() {
        j {
            val fields = listOf("hello" to string("world"), "hello2" to string("world2"))
            assertThat(fields(obj(fields)).toList(), equalTo(fields))
        }
    }

    @Test
    fun `get values`() {
        j {
            assertThat(text(string("world")), equalTo("world"))
            assertThat(integer(number(1)), equalTo(1L))
            assertThat(decimal(number(BigDecimal("1.0567"))), equalTo(BigDecimal("1.0567")))
            assertThat(bool(boolean(true)), equalTo(true))
        }
    }

    @Test
    fun `get string value`() {
        j {
            assertThat(textValueOf(obj("value" to string("world")), "value"), equalTo("world"))
            assertThat(textValueOf(obj("value" to number(1)), "value"), equalTo("1"))
            assertThat(textValueOf(obj("value" to boolean(true)), "value"), equalTo("true"))
        }
    }

    @Test
    fun `get elements`() {
        j {
            val fields = listOf(string("world"), string("world2"))
            val elements = elements(array(fields)).toList()
            assertThat(elements, equalTo(fields))
        }
    }

    @Test
    fun `can write and read spec as json`() {
        j {
            val validValue = """{"hello":"world"}"""
            checkContract(lens(spec), obj("hello" to string("world")), validValue, "", "hello", "o", "o$validValue", "o$validValue$validValue")
        }
    }

    @Test
    fun `invalid json blows up parse`() {
        j {
            assertThat({ parse("") }, throws(anything))
            assertThat({ parse("somevalue") }, throws(anything))
        }
    }

    @Test
    fun `get no fields`() {
        j {
            assertThat(fields(string("foo")).toList(), equalTo(emptyList()))
            assertThat(fields(boolean(true)).toList(), equalTo(emptyList()))
            assertThat(fields(number(123)).toList(), equalTo(emptyList()))
            assertThat(fields(nullNode()).toList(), equalTo(emptyList()))
        }
    }

    @Test
    fun compactify() {
        j {
            assertThat(compactify("""{  "hello"  :  "world"   }"""), equalTo("""{"hello":"world"}"""))
        }
    }

    @Test
    fun prettify() {
        j {
            assertThat(prettify("""{"hello":"world"}"""), equalTo(prettyString))
        }
    }
}
