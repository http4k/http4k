package org.http4k.mcp

import com.fasterxml.jackson.databind.JsonNode
import dev.forkhandles.result4k.flatMap
import dev.forkhandles.result4k.get
import dev.forkhandles.result4k.mapFailure
import dev.forkhandles.result4k.resultFrom
import org.http4k.connect.mcp.Content
import org.http4k.connect.mcp.McpTool
import org.http4k.connect.mcp.util.McpJson
import org.http4k.contract.jsonschema.v3.AutoJsonToJsonSchema
import org.http4k.core.Request
import org.http4k.jsonrpc.ErrorMessage.Companion.InternalError
import org.http4k.jsonrpc.ErrorMessage.Companion.InvalidParams
import org.http4k.mcp.tools.Tool
import org.http4k.mcp.tools.ToolHandler
import org.http4k.mcp.tools.ToolRequest
import org.http4k.mcp.tools.ToolResponse.Error
import org.http4k.mcp.tools.ToolResponse.Ok

class RoutedToolBinding<IN : Any>(
    val tool: Tool<IN>,
    private val handler: ToolHandler<IN>,
) : McpBinding {

    fun toTool() = McpTool(tool.name, tool.description, McpJson.convert(schema.asSchema(tool.example)))

    operator fun invoke(arguments: Map<String, Any>, connectRequest: Request) = resultFrom {
        with(McpJson) { ToolRequest(asA(asFormatString(arguments), tool.example::class), connectRequest) }
    }
        .mapFailure { Error(InvalidParams) }
        .flatMap { resultFrom { handler(it) }.mapFailure { Error(InternalError) } }
        .get()
        .let {
            McpTool.Call.Response(
                when (it) {
                    is Ok -> it.content
                    is Error -> listOf(Content.Text("ERROR: " + it.error.code + " " + it.error.message))
                },
                it is Error,
                it.meta
            )
        }

    private val schema = AutoJsonToJsonSchema(McpJson)

    private fun <IN : Any> AutoJsonToJsonSchema<JsonNode>.asSchema(input: IN) =
        toSchema(input).definitions.first { it.first == input::class.simpleName!! }.second
}
