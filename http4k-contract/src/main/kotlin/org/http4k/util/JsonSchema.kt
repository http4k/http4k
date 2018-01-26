package org.http4k.util

import org.http4k.format.Json
import org.http4k.format.JsonType
import org.http4k.lens.ParamMeta
import org.http4k.lens.ParamMeta.BooleanParam
import org.http4k.lens.ParamMeta.IntegerParam
import org.http4k.lens.ParamMeta.NumberParam
import org.http4k.lens.ParamMeta.StringParam

class IllegalSchemaException(message: String) : Exception(message)

data class JsonSchema<out NODE>(val node: NODE, val definitions: List<Pair<String, NODE>>)

class JsonToJsonSchema<ROOT : NODE, NODE : Any>(private val json: Json<ROOT, NODE>) {
    fun toSchema(node: NODE, overrideDefinitionId: String? = null) = JsonSchema(node, emptyList()).toSchema(overrideDefinitionId)

    private fun JsonSchema<NODE>.toSchema(overrideDefinitionId: String? = null): JsonSchema<NODE> =
        when (json.typeOf(node)) {
            JsonType.Object -> objectSchema(overrideDefinitionId)
            JsonType.Array -> arraySchema()
            JsonType.String -> JsonSchema(StringParam.schema(), definitions)
            JsonType.Number -> numberSchema()
            JsonType.Boolean -> JsonSchema(BooleanParam.schema(), definitions)
            JsonType.Null -> throw IllegalSchemaException("Cannot use a null value in a schema!")
            else -> throw IllegalSchemaException("unknown type")
        }

    private fun JsonSchema<NODE>.numberSchema(): JsonSchema<NODE> =
        JsonSchema((if (json.text(node).contains(".")) NumberParam else IntegerParam).schema(), definitions)

    private fun JsonSchema<NODE>.arraySchema(): JsonSchema<NODE> {
        val (node, definitions) = json.elements(node).toList().firstOrNull()?.let {
            JsonSchema(it, definitions).toSchema()
        } ?: throw IllegalSchemaException("Cannot use an empty list to generate a schema!")
        return JsonSchema(json.obj("type" to json.string("array"), "items" to node), definitions)
    }

    private fun JsonSchema<NODE>.objectSchema(overrideDefinitionId: String?): JsonSchema<NODE> {
        val (fields, subDefinitions) = json.fields(node).fold(listOf<Pair<String, NODE>>() to definitions, { (memoFields, memoDefinitions), (first, second) ->
            JsonSchema(second, memoDefinitions).toSchema().let { memoFields.plus(first to it.node) to it.definitions }
        })

        val newDefinition = json.obj("type" to json.string("object"), "properties" to json.obj(fields))
        val definitionId = overrideDefinitionId ?: "object" + newDefinition.hashCode()
        val allDefinitions = subDefinitions.plus(definitionId to newDefinition)
        return JsonSchema(json.obj("\$ref" to json.string("#/definitions/$definitionId")), allDefinitions)
    }

    private fun ParamMeta.schema(): NODE = json.obj("type" to json.string(value))
}

