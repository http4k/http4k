package org.http4k.util

interface JsonSchemaCreator<in IN, OUT> {
    fun toSchema(node: IN, overrideDefinitionId: String? = null): JsonSchema<OUT>
}

data class JsonSchema<out NODE>(val node: NODE, val definitions: Set<Pair<String, NODE>>)

class IllegalSchemaException(message: String) : Exception(message)
