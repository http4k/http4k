package org.http4k.mcp

import com.fasterxml.jackson.databind.JsonNode
import org.http4k.connect.mcp.Content
import org.http4k.connect.mcp.Tool
import org.http4k.connect.mcp.util.McpJson
import org.http4k.contract.jsonschema.v3.AutoJsonToJsonSchema

class ToolBinding<IN : Any>(
    val name: String,
    private val description: String,
    private val example: IN,
    private val fn: (IN) -> List<Content>,
) : McpBinding {
    private val schema = AutoJsonToJsonSchema(McpJson)

    fun toTool(): Tool = try {
        Tool(name, description, McpJson.convert(schema.asSchema(example)))
    } catch (e: Exception) {
        throw e
    }

    fun call(arguments: Map<String, Any>) =
        runCatching { with(McpJson) { fn(asA(asFormatString(arguments), example::class)) } }
            .map { Tool.Call.Response(it, false) }
            .getOrElse { Tool.Call.Response(emptyList(), true) }

    private fun AutoJsonToJsonSchema<JsonNode>.asSchema(input: IN) =
        toSchema(input).definitions.first { it.first == input::class.simpleName!! }.second
}
