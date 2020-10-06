package org.http4k.contract.openapi.v3

import org.http4k.format.Json
import org.http4k.format.JsonType
import org.http4k.lens.ParamMeta
import org.http4k.util.IllegalSchemaException
import org.http4k.util.JsonSchema
import org.http4k.util.JsonSchemaCreator

class JsonToJsonSchema<NODE>(
    private val json: Json<NODE>,
    private val refPrefix: String = "components/schemas"
) : JsonSchemaCreator<NODE, NODE> {
    override fun toSchema(obj: NODE, overrideDefinitionId: String?) = JsonSchema(obj, emptySet()).toSchema(overrideDefinitionId)

    private fun JsonSchema<NODE>.toSchema(overrideDefinitionId: String? = null): JsonSchema<NODE> =
        when (json.typeOf(node)) {
            JsonType.Object -> objectSchema(overrideDefinitionId)
            JsonType.Array -> arraySchema(overrideDefinitionId)
            JsonType.String -> JsonSchema(ParamMeta.StringParam.schema(json.string(json.text(node))), definitions)
            JsonType.Integer -> numberSchema()
            JsonType.Number -> numberSchema()
            JsonType.Boolean -> JsonSchema(ParamMeta.BooleanParam.schema(json.boolean(json.bool(node))), definitions)
            JsonType.Null -> throw IllegalSchemaException("Cannot use a null value in a schema!")
            else -> throw IllegalSchemaException("unknown type")
        }

    private fun JsonSchema<NODE>.numberSchema(): JsonSchema<NODE> {
        val text = json.text(node)
        val schema = when {
            text.contains(".") -> ParamMeta.NumberParam.schema(json.number(text.toBigDecimal()))
            else -> ParamMeta.IntegerParam.schema(json.number(text.toBigInteger()))
        }
        return JsonSchema(schema, definitions)
    }

    private fun JsonSchema<NODE>.arraySchema(overrideDefinitionId: String?): JsonSchema<NODE> {
        val (node, definitions) = json.elements(node).toList().firstOrNull()?.let {
            JsonSchema(it, definitions).toSchema(overrideDefinitionId)
        } ?: throw IllegalSchemaException("Cannot use an empty list to generate a schema!")
        return JsonSchema(json { obj("type" to string("array"), "items" to node) }, definitions)
    }

    private fun JsonSchema<NODE>.objectSchema(overrideDefinitionId: String?): JsonSchema<NODE> {
        val (fields, subDefinitions) = json.fields(node)
            .filter { json.typeOf(it.second) != JsonType.Null } // filter out null fields for which type can't be inferred
            .fold(listOf<Pair<String, NODE>>() to definitions) { (memoFields, memoDefinitions), (first, second) ->
                JsonSchema(second, memoDefinitions).toSchema().let { memoFields + (first to it.node) to it.definitions }
            }

        val newDefinition = json {
            obj(
                "type" to string("object"),
                "required" to array(emptyList()),
                "properties" to obj(fields)
            )
        }
        val definitionId = overrideDefinitionId ?: "object" + newDefinition.hashCode()
        val allDefinitions = subDefinitions + (definitionId to newDefinition)
        return JsonSchema(json { obj("\$ref" to string("#/$refPrefix/$definitionId")) }, allDefinitions)
    }

    private fun ParamMeta.schema(example: NODE): NODE = json { obj("type" to string(value), "example" to example) }
}
