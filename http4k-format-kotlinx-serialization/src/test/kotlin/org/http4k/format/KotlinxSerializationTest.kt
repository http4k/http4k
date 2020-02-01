package org.http4k.format

import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.equalTo
import kotlinx.serialization.json.JsonElement
import org.junit.jupiter.api.Test
import java.math.BigDecimal
import java.math.BigInteger

class KotlinxSerializationTest : JsonContract<JsonElement>(KotlinxSerialization) {
    override val prettyString = """{
	"hello": "world"
}"""

    /**
     * Overridden to allow for BigDecimal being serialized as a string. This is a shortcoming of kotlin.serialization
     * which is currently not customisable.
     */
    @Test
    override fun `serializes object to json`() {
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
            val expected = """{"string":"value","double":1.5,"long":10,"boolean":true,"bigDec":"1.1999999999999999555910790149937383830547332763671875","bigInt":12344,"null":null,"int":2,"empty":{},"array":["",123],"singletonArray":[{"number":123}]}"""
            assertThat(compact(input), equalTo(expected))
        }
    }
}

class KotlinxSerializationGenerateDataClassesTest : GenerateDataClassesContract<JsonElement>(KotlinxSerialization)
