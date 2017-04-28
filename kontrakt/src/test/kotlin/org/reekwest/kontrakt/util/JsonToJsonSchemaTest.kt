package org.reekwest.kontrakt.util

import argo.jdom.JsonNode
import argo.jdom.JsonNodeFactories.array
import argo.jdom.JsonNodeFactories.number
import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.equalTo
import org.junit.Test
import org.reekwest.kontrakt.formats.Argo.asJson
import org.reekwest.kontrakt.formats.Argo.fromJson
import org.reekwest.kontrakt.formats.Argo.obj
import java.io.InputStream
import java.math.BigDecimal


class JsonToJsonSchemaTest {

    fun String.readResource(): InputStream = JsonToJsonSchemaTest::class.java.getResourceAsStream(this)
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
        val expected: JsonNode = "JsonToJsonSchema_main.json".readResource().asJson()
        assertThat(actual.node, equalTo(expected))
        val expectedDefs: JsonNode = "JsonToJsonSchema_definitions.json".readResource().asJson()
        assertThat(obj(actual.definitions), equalTo(expectedDefs))
    }
}

