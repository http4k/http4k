package org.http4k.routing

import dev.forkhandles.result4k.flatMap
import dev.forkhandles.result4k.get
import dev.forkhandles.result4k.mapFailure
import dev.forkhandles.result4k.resultFrom
import org.http4k.contract.jsonschema.v3.AutoJsonToJsonSchema
import org.http4k.core.Request
import org.http4k.jsonrpc.ErrorMessage.Companion.InternalError
import org.http4k.jsonrpc.ErrorMessage.Companion.InvalidParams
import org.http4k.mcp.ToolHandler
import org.http4k.mcp.ToolRequest
import org.http4k.mcp.ToolResponse.Error
import org.http4k.mcp.ToolResponse.Ok
import org.http4k.mcp.model.Content
import org.http4k.mcp.model.Tool
import org.http4k.mcp.protocol.McpTool
import org.http4k.mcp.util.McpJson
import org.http4k.mcp.util.McpNodeType

class ToolFeatureBinding<IN : Any>(
    private val tool: Tool<IN>,
    private val handler: ToolHandler<IN>,
) : FeatureBinding {

    fun toTool() =
        McpTool(
            tool.name, tool.description, McpJson.convert(
                AutoJsonToJsonSchema(McpJson).asSchema(tool.example)
            )
        )

    fun call(mcp: McpTool.Call.Request, http: Request) = resultFrom {
        with(McpJson) { ToolRequest(asA(asFormatString(mcp.arguments), tool.example::class), http) }
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
}

fun <IN : Any> AutoJsonToJsonSchema<McpNodeType>.asSchema(input: IN) =
    toSchema(input).definitions.first { it.first == input::class.simpleName!! }.second
