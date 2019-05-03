package org.http4k.util

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.module.jsonSchema.JsonSchemaGenerator
import org.http4k.format.ConfigurableJackson
import org.http4k.format.Jackson

class JacksonJsonSchemaCreator(private val json: ConfigurableJackson = Jackson) : JsonSchemaCreator<Any, JsonNode> {
    private val jsonSchemaGenerator = JsonSchemaGenerator(json.mapper)

    override fun toSchema(obj: Any, overrideDefinitionId: String?): JsonSchema<JsonNode> {
        val node = jsonSchemaGenerator.generateSchema(obj::class.java)
        return with(json.asJsonString(node)) {
            JsonSchema(
                json {
                    obj("\$ref" to string("#/definitions/${node.id}"))
                },
                setOf("" to json.parse(replace("\"type\":\"any\"", "\"type\":\"string\""))))
        }
    }
}