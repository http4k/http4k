package org.http4k.util

import argo.jdom.JsonNode
import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.equalTo
import org.http4k.format.Argo
import org.http4k.format.Argo.obj
import org.junit.Test
import java.io.InputStream
import java.math.BigDecimal

class JsonToJsonSchemaTest {
    private val json = Argo

    fun String.readResource(): InputStream = JsonToJsonSchemaTest::class.java.getResourceAsStream(this)
    fun InputStream.asJsonValue() = json.parse(String(this.readBytes()))

    @Test
    fun `renders all different types of json value as expected`() {
        val model = json.obj(
            "aString" to json.string("aStringValue"),
            "aNumber" to json.number(BigDecimal(1.9)),
            "aBooleanTrue" to json.boolean(true),
            "aBooleanFalse" to json.boolean(false),
            "anArray" to json.array(listOf(json.obj("anotherString" to json.string("yetAnotherString")))),
            "anObject" to json.obj("anInteger" to json.number(1))
        )

        val actual = JsonToJsonSchema(json).toSchema(model)
        val expected: JsonNode = "JsonSchema_main.json".readResource().asJsonValue()
        assertThat(actual.node, equalTo(expected))
        val expectedDefs: JsonNode = "JsonSchema_definitions.json".readResource().asJsonValue()
//        println(obj(actual.definitions).asPrettyJsonString())
        assertThat(obj(actual.definitions), equalTo(expectedDefs))
    }
}

