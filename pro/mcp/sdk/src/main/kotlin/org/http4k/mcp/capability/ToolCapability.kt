package org.http4k.mcp.capability

import dev.forkhandles.result4k.get
import dev.forkhandles.result4k.map
import dev.forkhandles.result4k.mapFailure
import dev.forkhandles.result4k.resultFrom
import org.http4k.core.Request
import org.http4k.format.MoshiArray
import org.http4k.format.MoshiBoolean
import org.http4k.format.MoshiDecimal
import org.http4k.format.MoshiInteger
import org.http4k.format.MoshiLong
import org.http4k.format.MoshiNode
import org.http4k.format.MoshiNull
import org.http4k.format.MoshiObject
import org.http4k.format.MoshiString
import org.http4k.jsonrpc.ErrorMessage.Companion.InternalError
import org.http4k.jsonrpc.ErrorMessage.Companion.InvalidParams
import org.http4k.lens.LensFailure
import org.http4k.mcp.ToolHandler
import org.http4k.mcp.ToolRequest
import org.http4k.mcp.ToolResponse.Error
import org.http4k.mcp.ToolResponse.Ok
import org.http4k.mcp.model.Content.Text
import org.http4k.mcp.model.Tool
import org.http4k.mcp.protocol.messages.McpTool

class ToolCapability(private val tool: Tool, private val handler: ToolHandler) : ServerCapability {

    fun toTool() = McpTool(tool.name, tool.description, tool.toSchema())

    fun call(mcp: McpTool.Call.Request, http: Request) =
        resultFrom { ToolRequest(mcp.arguments.coerceIntoStrings(), http) }
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
                        is Error -> listOf(Text("ERROR: " + it.error.code + " " + it.error.message))
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

private fun Map<String, MoshiNode>.coerceIntoStrings() =
    mapNotNull { it.value.asString()?.let { value -> it.key to value } }.toMap()

private fun MoshiNode.asString(): Any? = when (this) {
    MoshiNull -> null
    is MoshiArray -> elements.mapNotNull { it.asString() }
    is MoshiBoolean -> value.toString()
    is MoshiString -> value
    is MoshiDecimal -> value.toString()
    is MoshiInteger -> value.toString()
    is MoshiLong -> value.toString()
    is MoshiObject -> attributes.mapValues { it.value.asString() }
}
