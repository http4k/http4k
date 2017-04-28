package org.reekwest.kontrakt.util

import argo.jdom.JsonNode
import argo.jdom.JsonNodeType.ARRAY
import argo.jdom.JsonNodeType.FALSE
import argo.jdom.JsonNodeType.NULL
import argo.jdom.JsonNodeType.NUMBER
import argo.jdom.JsonNodeType.OBJECT
import argo.jdom.JsonNodeType.STRING
import argo.jdom.JsonNodeType.TRUE
import org.reekwest.kontrakt.formats.Argo.asJson
import org.reekwest.kontrakt.formats.Argo.obj
import org.reekwest.kontrakt.util.ParamType.BooleanParamType

class IllegalSchemaException(message: String) : Exception(message)

data class Schema(val node: JsonNode, val definitions: Iterable<Pair<String, JsonNode>>)

fun JsonNode.toSchema(): Schema = toSchema(Schema(this, emptyList()))

private fun toSchema(input: Schema): Schema =
    if (input.node.type == STRING) Schema(paramTypeSchema(ParamType.StringParamType), input.definitions)
    else if (input.node.type == TRUE) Schema(paramTypeSchema(BooleanParamType), input.definitions)
    else if (input.node.type == FALSE) Schema(paramTypeSchema(BooleanParamType), input.definitions)
    else if (input.node.type == NUMBER) numberSchema(input)
    else if (input.node.type == ARRAY) arraySchema(input)
    else if (input.node.type == OBJECT) objectSchema(input)
    else if (input.node.type == NULL) throw IllegalSchemaException("Cannot use a null value in a schema!")
    else throw IllegalSchemaException("unknown type")

private fun paramTypeSchema(paramType: ParamType): JsonNode = obj("type" to paramType.name.asJson())

private fun numberSchema(input: Schema): Schema =
    Schema(paramTypeSchema(if (input.node.text.contains(".")) ParamType.NumberParamType else ParamType.IntegerParamType), input.definitions)

private fun arraySchema(input: Schema): Schema {
    val (node, definitions) = input.node.elements.getOrNull(0)?.let { toSchema(Schema(it, input.definitions)) } ?: throw
    IllegalSchemaException("Cannot use an empty list to generate a schema!")
    return Schema(obj("type" to "array".asJson(), "items" to node), definitions)
}

private fun objectSchema(input: Schema): Schema {
    val (fields, subDefinitions) = input.node.fieldList.fold(listOf<Pair<String, JsonNode>>() to input.definitions, {
        (memoFields, memoDefinitions), nextField ->
        val next = toSchema(Schema(nextField.value, memoDefinitions))
        memoFields.plus(nextField.name.text to next.node) to next.definitions
    })


    val newDefinition = obj("type" to "object".asJson(), "properties" to obj(fields))
    val definitionId = "object" + newDefinition.hashCode()
    val allDefinitions = subDefinitions.plus(definitionId to newDefinition)
    return Schema(obj("\$ref" to "#/definitions/$definitionId".asJson()), allDefinitions)
}


