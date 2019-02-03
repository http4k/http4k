package org.http4k.util

import com.fasterxml.jackson.databind.JsonNode
import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.equalTo
import org.http4k.format.Jackson
import org.http4k.format.Jackson.obj
import org.junit.jupiter.api.Test
import java.io.InputStream
import java.math.BigDecimal

class JsonToJsonSchemaTest {
    private val json = Jackson

    private fun String.readResource(): InputStream = JsonToJsonSchemaTest::class.java.getResourceAsStream(this)
    private fun InputStream.asJsonValue() = json.parse(String(readBytes()))

    @Test
    fun `renders all different types of json value as expected`() {
        val model = json {
            obj(
                "aString" to string("aStringValue"),
                "aNumber" to number(BigDecimal("1.9")),
                "aBooleanTrue" to boolean(true),
                "aBooleanFalse" to boolean(false),
                "anArray" to array(listOf(obj("anotherString" to string("yetAnotherString")))),
                "anObject" to obj("anInteger" to number(1)),
                "anotherObject" to obj("anInteger" to number(1))
            )
        }

        val actual = JsonToJsonSchema(json).toSchema(model, "bob")
        val expected: JsonNode = "JsonSchema_main.json".readResource().asJsonValue()
        assertThat(actual.node, equalTo(expected))
        val expectedDefs = "JsonSchema_definitions.json".readResource().asJsonValue()
//        println(json.pretty(obj(actual.definitions)))
        assertThat(json.pretty(obj(actual.definitions)), equalTo(json.pretty(expectedDefs)))
    }
}
