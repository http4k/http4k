package org.reekwest.http.contract.util

import argo.jdom.JsonNode
import argo.jdom.JsonNodeFactories.array
import argo.jdom.JsonNodeFactories.number
import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.equalTo
import org.junit.Test
import org.reekwest.http.contract.formats.Argo.asJson
import org.reekwest.http.contract.formats.Argo.fromJson
import org.reekwest.http.contract.formats.Argo.obj
import java.io.InputStream
import java.math.BigDecimal

class JsonSchemaTest {

    fun String.readResource(): InputStream = JsonSchemaTest::class.java.getResourceAsStream(this)
    fun InputStream.asJson() = String(this.readBytes()).fromJson()

    @Test
    fun `renders all different types of json value as expected`() {
        val model = obj(
            "aString" to "aStringValue".asJson(),
            "aNumber" to BigDecimal(1.9).asJson(),
            "aBooleanTrue" to true.asJson(),
            "aBooleanFalse" to false.asJson(),
            "anArray" to array(obj("anotherString" to "yetAnotherString".asJson())),
            "anObject" to obj("anInteger" to number(1))
        )

        val actual = model.toSchema()
        val expected: JsonNode = "JsonSchema_main.json".readResource().asJson()
        assertThat(actual.node, equalTo(expected))
        val expectedDefs: JsonNode = "JsonSchema_definitions.json".readResource().asJson()
        assertThat(obj(actual.definitions), equalTo(expectedDefs))
    }
}

