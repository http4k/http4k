package org.http4k.ai.mcp.server.capability

import dev.forkhandles.result4k.get
import dev.forkhandles.result4k.map
import dev.forkhandles.result4k.mapFailure
import dev.forkhandles.result4k.resultFrom
import org.http4k.ai.mcp.Client
import org.http4k.ai.mcp.ToolHandler
import org.http4k.ai.mcp.ToolRequest
import org.http4k.ai.mcp.ToolResponse.ElicitationRequired
import org.http4k.ai.mcp.ToolResponse.Error
import org.http4k.ai.mcp.ToolResponse.Ok
import org.http4k.ai.mcp.ToolResponse.Task
import org.http4k.ai.mcp.model.Content.Text
import org.http4k.ai.mcp.model.Tool
import org.http4k.ai.mcp.protocol.McpException
import org.http4k.ai.mcp.protocol.messages.McpTool
import org.http4k.ai.mcp.protocol.messages.URLElicitationRequiredError
import org.http4k.ai.mcp.util.McpJson
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

interface ToolCapability : ServerCapability, ToolHandler {
    fun toTool(): McpTool
    fun call(mcp: McpTool.Call.Request, client: Client, http: Request): McpTool.Call.Response
}

fun ToolCapability(tool: Tool, handler: ToolHandler) = object : ToolCapability {
    override fun toTool() = McpTool(
        tool.name, tool.description,
        tool.title,
        McpJson.convert(tool.toSchema()),
        tool.output?.toSchema()?.let { McpJson.convert(it) },
        tool.annotations,
        tool.icons,
        tool.execution,
        tool.meta
    )

    override fun call(mcp: McpTool.Call.Request, client: Client, http: Request) =
        resultFrom { ToolRequest(mcp.arguments.coerceIntoRawTypes(), mcp._meta, mcp.task, client, http) }
            .mapFailure { throw McpException(InvalidParams) }
            .map {
                try {
                    this(it)
                } catch (_: LensFailure) {
                    throw McpException(InvalidParams)
                } catch (_: Exception) {
                    Error(InternalError)
                }
            }
            .get()
            .let {
                when (it) {
                    is Ok -> McpTool.Call.Response(
                        content = it.content,
                        structuredContent = it.structuredContent?.let(McpJson::convert),
                        isError = false,
                        _meta = it.meta
                    )

                    is Error -> McpTool.Call.Response(
                        content = listOf(Text("ERROR: " + it.error.code + " " + it.error.message)),
                        isError = true,
                        _meta = it.meta
                    )

                    is Task -> McpTool.Call.Response(
                        task = it.task,
                        _meta = it.meta
                    )

                    is ElicitationRequired -> throw McpException(
                        URLElicitationRequiredError(it.elicitations, it.message)
                    )
                }
            }

    override fun invoke(p1: ToolRequest) = handler(p1)
}

private fun Map<String, MoshiNode>.coerceIntoRawTypes() =
    mapNotNull { it.value.asString()?.let { value -> it.key to value } }.toMap()

private fun MoshiNode.asString(): Any? = when (this) {
    MoshiNull -> null
    is MoshiArray -> elements.mapNotNull { it.asString() }
    is MoshiObject -> attributes.mapValues { it.value.asString() }
    is MoshiBoolean -> value
    is MoshiString -> value
    is MoshiDecimal -> value
    is MoshiInteger -> value
    is MoshiLong -> value
}

fun Tool.toSchema() = McpJson {
    obj(
        "type" to string("object"),
        "required" to array(args.filter { it.meta.required }.map { string(it.meta.name) }),
        "properties" to obj(args.map { it.meta.name to it.toSchema() })
    )
}
