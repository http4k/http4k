package org.http4k.contract.jsonschema.v3

import com.ubertob.kondor.json.jsonnode.JsonNode
import org.http4k.contract.jsonschema.JsonSchema
import org.http4k.contract.jsonschema.JsonSchemaCreator
import org.http4k.format.KondorJson

class KondorJsonSchemaCreator(
    private val json: KondorJson,
    private val refLocationPrefix: String = "components/schema",
) : JsonSchemaCreator<Any, JsonNode> {
    private val delegate = JsonToJsonSchema(json)

    override fun toSchema(obj: Any, overrideDefinitionId: String?, refModelNamePrefix: String?): JsonSchema<JsonNode> {
        try {
            if (obj is JsonNode) return delegate.toSchema(obj, overrideDefinitionId, refModelNamePrefix)
            if (obj is Enum<*>) return toEnumSchema(obj, refModelNamePrefix, overrideDefinitionId)

            val schema = json.converterFor(obj).schema()
            val definitionId = overrideDefinitionId ?: obj::class.simpleName ?: return JsonSchema(schema)

            val reference = (refModelNamePrefix ?: "") + definitionId
            val schemaRef = json {
                obj("\$ref" to string("#/$refLocationPrefix/$reference"))
            }

            return JsonSchema(schemaRef, setOf(reference to schema))
        } catch (e: Exception) {
            return delegate.toSchema(json.obj(), overrideDefinitionId, refModelNamePrefix)
        }
    }

    private fun toEnumSchema(
        obj: Enum<*>,
        refModelNamePrefix: String?,
        overrideDefinitionId: String?,
    ): JsonSchema<JsonNode> {
        val newDefinition = json.obj(
            "example" to json.string(obj.name),
            "type" to json.string("string"),
            "enum" to json.array(obj.javaClass.enumConstants.map { json.string(it.name) })
        )
        val definitionId =
            (refModelNamePrefix.orEmpty()) + (overrideDefinitionId ?: ("object" + newDefinition.hashCode()))
        return JsonSchema(
            json { obj("\$ref" to string("#/$refLocationPrefix/$definitionId")) },
            setOf(definitionId to newDefinition)
        )
    }
}
