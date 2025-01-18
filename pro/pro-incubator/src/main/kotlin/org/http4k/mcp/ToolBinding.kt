package org.http4k.mcp

import com.fasterxml.jackson.databind.JsonNode
import dev.forkhandles.result4k.flatMap
import dev.forkhandles.result4k.get
import dev.forkhandles.result4k.mapFailure
import dev.forkhandles.result4k.resultFrom
import org.http4k.connect.mcp.Content
import org.http4k.connect.mcp.HasMeta.Companion.default
import org.http4k.connect.mcp.Meta
import org.http4k.connect.mcp.Tool
import org.http4k.connect.mcp.util.McpJson
import org.http4k.contract.jsonschema.v3.AutoJsonToJsonSchema
import org.http4k.core.Request
import org.http4k.jsonrpc.ErrorMessage
import org.http4k.jsonrpc.ErrorMessage.Companion.InternalError
import org.http4k.jsonrpc.ErrorMessage.Companion.InvalidParams
import org.http4k.mcp.ToolResponse.Error
import org.http4k.mcp.ToolResponse.Ok

typealias ToolHandler<Input> = (ToolRequest<Input>) -> ToolResponse

data class ToolRequest<Input>(val input: Input, val connectRequest: Request)

sealed interface ToolResponse {
    val meta: Meta

    data class Ok(val content: List<Content>, override val meta: Meta = default) : ToolResponse
    data class Error(val error: ErrorMessage, override val meta: Meta = default) : ToolResponse
}

class ToolBinding<IN : Any>(
    val name: String,
    private val description: String,
    private val example: IN,
    private val handler: ToolHandler<IN>,
) : McpBinding {
    private val schema = AutoJsonToJsonSchema(McpJson)

    fun toTool() = Tool(name, description, McpJson.convert(schema.asSchema(example)))

    fun call(arguments: Map<String, Any>, connectRequest: Request): Tool.Call.Response = with(McpJson) {
        val resp = resultFrom { ToolRequest(asA(asFormatString(arguments), example::class), connectRequest) }
            .mapFailure { Error(InvalidParams) }
            .flatMap { resultFrom { handler(it) }.mapFailure { Error(InternalError) } }
            .get()
        return Tool.Call.Response(
            when (resp) {
                is Ok -> resp.content
                is Error -> listOf(Content.Text("ERROR: " + resp.error.code + " " + resp.error.message))
            },
            resp is Error,
            resp.meta
        )
    }

    private fun AutoJsonToJsonSchema<JsonNode>.asSchema(input: IN) =
        toSchema(input).definitions.first { it.first == input::class.simpleName!! }.second
}
