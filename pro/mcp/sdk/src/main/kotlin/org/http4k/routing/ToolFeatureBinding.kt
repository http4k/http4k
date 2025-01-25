package org.http4k.routing

import dev.forkhandles.result4k.get
import dev.forkhandles.result4k.map
import dev.forkhandles.result4k.mapFailure
import dev.forkhandles.result4k.resultFrom
import org.http4k.core.Request
import org.http4k.jsonrpc.ErrorMessage.Companion.InternalError
import org.http4k.jsonrpc.ErrorMessage.Companion.InvalidParams
import org.http4k.lens.LensFailure
import org.http4k.mcp.ToolHandler
import org.http4k.mcp.ToolRequest
import org.http4k.mcp.ToolResponse.Error
import org.http4k.mcp.ToolResponse.Ok
import org.http4k.mcp.model.Content
import org.http4k.mcp.model.Tool
import org.http4k.mcp.protocol.McpTool

class ToolFeatureBinding(private val tool: Tool, private val handler: ToolHandler) : FeatureBinding {

    fun toTool() = McpTool(tool.name, tool.description, tool.toSchema())

    fun call(mcp: McpTool.Call.Request, http: Request) = resultFrom { ToolRequest(mcp.arguments, http) }
        .mapFailure { Error(InvalidParams) }
        .map {
            try {
                handler(it)
            } catch (e: LensFailure) {
                Error(InvalidParams)
            } catch (e: Exception) {
                Error(InternalError)
            }
        }
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

private fun Tool.toSchema() = mapOf(
    "type" to "object",
    "required" to args.filter { it.meta.required }.map { it.meta.name },
    "properties" to mapOf(
        *args.map {
            it.meta.name to mapOf(
                "type" to it.meta.paramMeta.description,
                "description" to it.meta.description,
            )
        }.toTypedArray()
    )
)
