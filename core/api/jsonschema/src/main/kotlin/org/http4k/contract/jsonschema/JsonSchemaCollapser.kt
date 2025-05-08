package org.http4k.contract.jsonschema

import org.http4k.format.Json
import org.http4k.format.JsonType
import org.http4k.format.JsonType.Object

class JsonSchemaCollapser<NODE : Any>(private val json: Json<NODE>) {
    fun collapseToNode(schema: JsonSchema<NODE>): NODE {
        val definitionsMap = json.fields(schema.definitions).toMap()

        val processedDefinitions = definitionsMap.mapValues { (_, node) ->
            collapseNodeWithRefs(node, definitionsMap, mutableSetOf())
        }

        return collapseNodeWithRefs(schema.node, processedDefinitions, mutableSetOf())
    }

    private val traverseAndReplaceChildren: (NODE, (NODE) -> NODE) -> NODE = { node, processChild ->
        when (json.typeOf(node)) {
            Object -> json.fields(node)
                .fold(json.obj()) { acc, (fieldName, field) ->
                    when {
                        fieldName != "\$ref" -> json.obj(json.fields(acc) + (fieldName to processChild(field)))
                        else -> acc
                    }
                }

            JsonType.Array -> json.array(json.elements(node).map(processChild))
            else -> node
        }
    }

    private fun collapseNodeWithRefs(node: NODE, definitions: Map<String, NODE>, visited: MutableSet<String>): NODE =
        when {
            json.isRefNode(node) -> {
                val refName = json.getRefName(node)

                if (refName in visited) throw IllegalStateException("Circular reference detected for $refName")

                visited.add(refName)

                val definitionNode = definitions[refName]
                    ?: throw IllegalArgumentException("Reference '$refName' not found in definitions")

                collapseNodeWithRefs(definitionNode, definitions, visited.toMutableSet())
            }

            else -> traverseAndReplaceChildren(node) { collapseNodeWithRefs(it, definitions, visited.toMutableSet()) }
        }

    private fun Json<NODE>.isRefNode(it: NODE) = textValueOf(it, "\$ref") != null

    private fun Json<NODE>.getRefName(it: NODE) = textValueOf(it, "\$ref")!!.substringAfterLast("/")
}
