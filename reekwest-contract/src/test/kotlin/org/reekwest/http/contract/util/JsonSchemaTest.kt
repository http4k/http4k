package org.reekwest.http.contract.util

import argo.jdom.JsonNode
import argo.jdom.JsonNodeFactories.array
import argo.jdom.JsonNodeFactories.number
import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.equalTo
import org.junit.Test
import org.reekwest.http.formats.Argo.asJsonValue
import org.reekwest.http.formats.Argo.fromJsonString
import org.reekwest.http.formats.Argo.obj
import java.io.InputStream
import java.math.BigDecimal

class JsonSchemaTest {

    fun String.readResource(): InputStream = JsonSchemaTest::class.java.getResourceAsStream(this)
    fun InputStream.asJsonValue() = String(this.readBytes()).fromJsonString()

    @Test
    fun `renders all different types of json value as expected`() {
        val model = obj(
            "aString" to "aStringValue".asJsonValue(),
            "aNumber" to BigDecimal(1.9).asJsonValue(),
            "aBooleanTrue" to true.asJsonValue(),
            "aBooleanFalse" to false.asJsonValue(),
            "anArray" to array(obj("anotherString" to "yetAnotherString".asJsonValue())),
            "anObject" to obj("anInteger" to number(1))
        )

        val actual = model.toSchema()
        val expected: JsonNode = "JsonSchema_main.json".readResource().asJsonValue()
        assertThat(actual.node, equalTo(expected))
        val expectedDefs: JsonNode = "JsonSchema_definitions.json".readResource().asJsonValue()
        assertThat(obj(actual.definitions), equalTo(expectedDefs))
    }
}

