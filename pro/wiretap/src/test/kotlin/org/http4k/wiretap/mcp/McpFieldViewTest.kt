package org.http4k.wiretap.mcp

import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.equalTo
import org.http4k.ai.mcp.protocol.messages.McpPrompt
import org.junit.jupiter.api.Test

class McpFieldViewTest {

    @Test
    fun `parses simple string properties from tool schema`() {
        val schema = mapOf(
            "type" to "object",
            "properties" to mapOf(
                "name" to mapOf("type" to "string", "description" to "The user name")
            ),
            "required" to listOf("name")
        )

        val fields = schema.toFieldViews()

        assertThat(
            fields, equalTo(
                listOf(
                    McpFieldView(
                        name = "name",
                        description = "The user name",
                        required = true,
                        type = "string",
                        enumValues = emptyList(),
                        defaultValue = ""
                    )
                )
            )
        )
    }

    @Test
    fun `parses multiple types including integer, number, boolean`() {
        val schema = mapOf(
            "type" to "object",
            "properties" to mapOf(
                "count" to mapOf("type" to "integer"),
                "ratio" to mapOf("type" to "number"),
                "enabled" to mapOf("type" to "boolean")
            )
        )

        val fields = schema.toFieldViews()

        assertThat(
            fields.map { it.name to it.type }, equalTo(
                listOf(
                    "count" to "integer",
                    "enabled" to "boolean",
                    "ratio" to "number"
                )
            )
        )
    }

    @Test
    fun `parses enum values`() {
        val schema = mapOf(
            "type" to "object",
            "properties" to mapOf(
                "color" to mapOf(
                    "type" to "string",
                    "enum" to listOf("red", "green", "blue")
                )
            )
        )

        val fields = schema.toFieldViews()

        assertThat(fields.first().enumValues, equalTo(listOf("red", "green", "blue")))
    }

    @Test
    fun `parses default values`() {
        val schema = mapOf(
            "type" to "object",
            "properties" to mapOf(
                "format" to mapOf(
                    "type" to "string",
                    "default" to "json"
                )
            )
        )

        val fields = schema.toFieldViews()

        assertThat(fields.first().defaultValue, equalTo("json"))
    }

    @Test
    fun `handles schema with no properties`() {
        val schema = mapOf("type" to "object")

        assertThat(schema.toFieldViews(), equalTo(emptyList()))
    }

    @Test
    fun `handles empty schema`() {
        val schema = emptyMap<String, Any>()

        assertThat(schema.toFieldViews(), equalTo(emptyList()))
    }

    @Test
    fun `optional fields have required false`() {
        val schema = mapOf(
            "type" to "object",
            "properties" to mapOf(
                "name" to mapOf("type" to "string"),
                "age" to mapOf("type" to "integer")
            ),
            "required" to listOf("name")
        )

        val fields = schema.toFieldViews()

        assertThat(fields.first { it.name == "name" }.required, equalTo(true))
        assertThat(fields.first { it.name == "age" }.required, equalTo(false))
    }

    @Test
    fun `converts prompt argument to field view`() {
        val arg = McpPrompt.Argument(
            name = "topic",
            description = "The topic to discuss",
            required = true
        )

        val field = arg.toFieldView()

        assertThat(
            field, equalTo(
                McpFieldView(
                    name = "topic",
                    description = "The topic to discuss",
                    required = true,
                    type = "string",
                    enumValues = emptyList(),
                    defaultValue = ""
                )
            )
        )
    }

    @Test
    fun `converts prompt argument with null description and required`() {
        val arg = McpPrompt.Argument(name = "query")

        val field = arg.toFieldView()

        assertThat(field.description, equalTo(""))
        assertThat(field.required, equalTo(false))
    }

    @Test
    fun `fields are sorted by name`() {
        val schema = mapOf(
            "type" to "object",
            "properties" to mapOf(
                "zebra" to mapOf("type" to "string"),
                "alpha" to mapOf("type" to "string"),
                "middle" to mapOf("type" to "string")
            )
        )

        val fields = schema.toFieldViews()

        assertThat(fields.map { it.name }, equalTo(listOf("alpha", "middle", "zebra")))
    }
}
