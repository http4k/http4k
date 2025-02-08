package org.http4k.contract.jsonschema

interface JsonSchemaCreator<IN, OUT> {
    fun toSchema(obj: IN, overrideDefinitionId: String? = null, refModelNamePrefix: String? = null): JsonSchema<OUT>
}

data class JsonSchema<out NODE>(val node: NODE, val definitions: Set<Pair<String, NODE>> = emptySet())

class IllegalSchemaException(message: String) : RuntimeException(message)
