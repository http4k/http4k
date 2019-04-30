package org.http4k.util

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.module.jsonSchema.JsonSchemaGenerator
import org.http4k.format.ConfigurableJackson

class JacksonJsonSchemaCreator(private val json: ConfigurableJackson) : JsonSchemaCreator<Any, JsonNode> {
    private val jsonSchemaGenerator = JsonSchemaGenerator(json.mapper)

    override fun toSchema(obj: Any, overrideDefinitionId: String?) =
        with(json.asJsonString(jsonSchemaGenerator.generateSchema(obj::class.java))) {
            JsonSchema(
                json.parse(this
                    .replace("\"type\":\"any\"", "\"type\":\"string\"")
                ), emptySet())
        }
}