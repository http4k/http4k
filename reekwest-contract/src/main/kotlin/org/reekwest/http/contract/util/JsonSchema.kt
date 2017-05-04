package org.reekwest.http.contract.util

import org.reekwest.http.contract.util.ParamType.BooleanParamType
import org.reekwest.http.contract.util.ParamType.IntegerParamType
import org.reekwest.http.contract.util.ParamType.NumberParamType
import org.reekwest.http.contract.util.ParamType.StringParamType
import org.reekwest.http.formats.Json
import org.reekwest.http.formats.JsonType

class IllegalSchemaException(message: String) : Exception(message)

data class JsonSchema<out NODE>(val node: NODE, val definitions: List<Pair<String, NODE>>)

class JsonToJsonSchema<ROOT : NODE, NODE : Any>(private val json: Json<ROOT, NODE>) {
    fun toSchema(node: NODE): JsonSchema<NODE> = toSchema(JsonSchema(node, emptyList()))

    private fun toSchema(input: JsonSchema<NODE>): JsonSchema<NODE> =
        when (json.typeOf(input.node)) {
            JsonType.Object -> objectSchema(input)
            JsonType.Array -> arraySchema(input)
            JsonType.String -> JsonSchema(paramTypeSchema(StringParamType), input.definitions)
            JsonType.Number -> numberSchema(input)
            JsonType.Boolean -> JsonSchema(paramTypeSchema(BooleanParamType), input.definitions)
            JsonType.Null -> throw IllegalSchemaException("Cannot use a null value in a schema!")
            else -> throw IllegalSchemaException("unknown type")
        }

    private fun paramTypeSchema(paramType: ParamType): NODE = json.obj("type" to json.string(paramType.name))

    private fun numberSchema(input: JsonSchema<NODE>): JsonSchema<NODE> =
        JsonSchema(paramTypeSchema(if (json.text(input.node).contains(".")) NumberParamType else IntegerParamType), input.definitions)

    private fun arraySchema(input: JsonSchema<NODE>): JsonSchema<NODE> {
        val (node, definitions) = json.elements(input.node).toList().firstOrNull()?.let { toSchema(JsonSchema(it, input.definitions)) } ?: throw
        IllegalSchemaException("Cannot use an empty list to generate a schema!")
        return JsonSchema(json.obj("type" to json.string("array"), "items" to node), definitions)
    }

    private fun objectSchema(input: JsonSchema<NODE>): JsonSchema<NODE> {
        val (fields, subDefinitions) = json.fields(input.node).fold(listOf<Pair<String, NODE>>() to input.definitions, {
            (memoFields, memoDefinitions), (first, second) ->
            val next = toSchema(JsonSchema(second, memoDefinitions))
            memoFields.plus(first to next.node) to next.definitions
        })

        val newDefinition = json.obj("type" to json.string("object"), "properties" to json.obj(fields))
        val definitionId = "object" + newDefinition.hashCode()
        val allDefinitions = subDefinitions.plus(definitionId to newDefinition)
        return JsonSchema(json.obj("\$ref" to json.string("#/definitions/$definitionId")), allDefinitions)
    }


}

