package org.reekwest.http.contract.util

import argo.jdom.JsonNode
import argo.jdom.JsonNodeType.ARRAY
import argo.jdom.JsonNodeType.FALSE
import argo.jdom.JsonNodeType.NULL
import argo.jdom.JsonNodeType.NUMBER
import argo.jdom.JsonNodeType.OBJECT
import argo.jdom.JsonNodeType.STRING
import argo.jdom.JsonNodeType.TRUE
import org.reekwest.http.contract.util.ParamType.BooleanParamType
import org.reekwest.http.contract.util.ParamType.IntegerParamType
import org.reekwest.http.contract.util.ParamType.NumberParamType
import org.reekwest.http.contract.util.ParamType.StringParamType
import org.reekwest.http.formats.Argo.asJson
import org.reekwest.http.formats.Argo.obj

class IllegalSchemaException(message: String) : Exception(message)

data class JsonSchema(val node: JsonNode, val definitions: Iterable<Pair<String, JsonNode>>)

fun JsonNode.toSchema(): JsonSchema = toSchema(JsonSchema(this, emptyList()))

private fun toSchema(input: JsonSchema): JsonSchema =
    if (input.node.type == STRING) JsonSchema(paramTypeSchema(StringParamType), input.definitions)
    else if (input.node.type == TRUE) JsonSchema(paramTypeSchema(BooleanParamType), input.definitions)
    else if (input.node.type == FALSE) JsonSchema(paramTypeSchema(BooleanParamType), input.definitions)
    else if (input.node.type == NUMBER) numberSchema(input)
    else if (input.node.type == ARRAY) arraySchema(input)
    else if (input.node.type == OBJECT) objectSchema(input)
    else if (input.node.type == NULL) throw IllegalSchemaException("Cannot use a null value in a schema!")
    else throw IllegalSchemaException("unknown type")

private fun paramTypeSchema(paramType: ParamType): JsonNode = obj("type" to paramType.name.asJson())

private fun numberSchema(input: JsonSchema): JsonSchema =
    JsonSchema(paramTypeSchema(if (input.node.text.contains(".")) NumberParamType else IntegerParamType), input.definitions)

private fun arraySchema(input: JsonSchema): JsonSchema {
    val (node, definitions) = input.node.elements.getOrNull(0)?.let { toSchema(JsonSchema(it, input.definitions)) } ?: throw
    IllegalSchemaException("Cannot use an empty list to generate a schema!")
    return JsonSchema(obj("type" to "array".asJson(), "items" to node), definitions)
}

private fun objectSchema(input: JsonSchema): JsonSchema {
    val (fields, subDefinitions) = input.node.fieldList.fold(listOf<Pair<String, JsonNode>>() to input.definitions, {
        (memoFields, memoDefinitions), nextField ->
        val next = toSchema(JsonSchema(nextField.value, memoDefinitions))
        memoFields.plus(nextField.name.text to next.node) to next.definitions
    })


    val newDefinition = obj("type" to "object".asJson(), "properties" to obj(fields))
    val definitionId = "object" + newDefinition.hashCode()
    val allDefinitions = subDefinitions.plus(definitionId to newDefinition)
    return JsonSchema(obj("\$ref" to "#/definitions/$definitionId".asJson()), allDefinitions)
}


