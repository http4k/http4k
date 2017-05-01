package org.reekwest.http.contract.util

import argo.jdom.JsonNode
import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.equalTo
import org.junit.Test
import org.reekwest.http.formats.Argo
import org.reekwest.http.formats.Argo.obj
import java.io.InputStream
import java.math.BigDecimal

class JsonSchemaTest {
    private val json = Argo

    fun String.readResource(): InputStream = JsonSchemaTest::class.java.getResourceAsStream(this)
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

        val actual = model.toSchema()
        val expected: JsonNode = "JsonSchema_main.json".readResource().asJsonValue()
        assertThat(actual.node, equalTo(expected))
        val expectedDefs: JsonNode = "JsonSchema_definitions.json".readResource().asJsonValue()
        assertThat(obj(actual.definitions), equalTo(expectedDefs))
    }
}

