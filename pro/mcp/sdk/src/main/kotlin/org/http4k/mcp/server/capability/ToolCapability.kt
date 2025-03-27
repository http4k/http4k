package org.http4k.mcp.server.capability

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
import org.http4k.mcp.model.asMcp
import org.http4k.mcp.protocol.McpException
import org.http4k.mcp.protocol.messages.McpTool

interface ToolCapability : ServerCapability, ToolHandler {
    fun toTool(): McpTool

    fun call(mcp: McpTool.Call.Request, http: Request): McpTool.Call.Response
}

fun ToolCapability(tool: Tool, handler: ToolHandler) = object : ToolCapability {
    override fun toTool() = tool.asMcp()

    override fun call(mcp: McpTool.Call.Request, http: Request) =
        resultFrom { ToolRequest(mcp.arguments.coerceIntoStrings(), mcp._meta.progress, http) }
            .mapFailure { throw McpException(InvalidParams) }
            .map {
                try {
                    handler(it)
                } catch (e: LensFailure) {
                    throw McpException(InvalidParams)
                } catch (e: Exception) {
                    e.printStackTrace()
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

    override fun invoke(p1: ToolRequest) = handler(p1)
}

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
