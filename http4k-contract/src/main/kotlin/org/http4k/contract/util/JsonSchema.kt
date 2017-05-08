package util

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
    fun toSchema(node: NODE): JsonSchema<NODE> = toSchema(JsonSchema(node, emptyList()))

    private fun toSchema(input: JsonSchema<NODE>): JsonSchema<NODE> =
        when (json.typeOf(input.node)) {
            JsonType.Object -> objectSchema(input)
            JsonType.Array -> arraySchema(input)
            JsonType.String -> JsonSchema(paramTypeSchema(StringParam), input.definitions)
            JsonType.Number -> numberSchema(input)
            JsonType.Boolean -> JsonSchema(paramTypeSchema(BooleanParam), input.definitions)
            JsonType.Null -> throw IllegalSchemaException("Cannot use a null value in a schema!")
            else -> throw IllegalSchemaException("unknown type")
        }

    private fun paramTypeSchema(paramMeta: ParamMeta): NODE = json.obj("type" to json.string(paramMeta.value))

    private fun numberSchema(input: JsonSchema<NODE>): JsonSchema<NODE> =
        JsonSchema(paramTypeSchema(if (json.text(input.node).contains(".")) NumberParam else IntegerParam), input.definitions)

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

