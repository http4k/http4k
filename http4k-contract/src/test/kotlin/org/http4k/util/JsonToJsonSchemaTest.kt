package org.http4k.util

import argo.jdom.JsonNode
import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.equalTo
import org.http4k.format.Argo
import org.http4k.format.Argo.obj
import org.junit.jupiter.api.Test
import java.io.InputStream
import java.math.BigDecimal

class JsonToJsonSchemaTest {
    private val json = Argo

    private fun String.readResource(): InputStream = JsonToJsonSchemaTest::class.java.getResourceAsStream(this)
    private fun InputStream.asJsonValue() = json.parse(String(readBytes()))

    @Test
    fun `renders all different types of json value as expected`() {
        val model = json.obj(
            "aString" to json.string("aStringValue"),
            "aNumber" to json.number(BigDecimal("1.9")),
            "aBooleanTrue" to json.boolean(true),
            "aBooleanFalse" to json.boolean(false),
            "anArray" to json.array(listOf(json.obj("anotherString" to json.string("yetAnotherString")))),
            "anObject" to json.obj("anInteger" to json.number(1)),
            "anotherObject" to json.obj("anInteger" to json.number(1))
        )

        val actual = JsonToJsonSchema(json).toSchema(model, "bob")
        val expected: JsonNode = "JsonSchema_main.json".readResource().asJsonValue()
        assertThat(actual.node, equalTo(expected))
        val expectedDefs = "JsonSchema_definitions.json".readResource().asJsonValue()
        println(json.pretty(obj(actual.definitions)))
        assertThat(json.pretty(obj(actual.definitions)), equalTo(json.pretty(expectedDefs)))
    }
}
