package org.http4k.util

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.module.jsonSchema.JsonSchemaGenerator
import org.http4k.format.ConfigurableJackson
import org.http4k.format.Jackson

class JacksonJsonSchemaCreator(private val json: ConfigurableJackson = Jackson,
                               private val refPrefix: String = "components/schemas"
) : JsonSchemaCreator<Any, JsonNode> {
    private val jsonToJsonSchema = JsonToJsonSchema(json, refPrefix)
    private val jsonSchemaGenerator = JsonSchemaGenerator(json.mapper)

    override fun toSchema(obj: Any, overrideDefinitionId: String?) =
        when (obj) {
            is JsonNode -> jsonToJsonSchema.toSchema(obj, overrideDefinitionId)
            else -> {
                val node = jsonSchemaGenerator.generateSchema(obj::class.java)
                val id = overrideDefinitionId ?: node.id
                with(json.asJsonString(node)) {
                    JsonSchema(
                        json { obj("\$ref" to string("#/$refPrefix/$id")) },
                        setOf(id to json.parse(replace("\"type\":\"any\"", "\"type\":\"string\""))))
                }
            }
        }
}