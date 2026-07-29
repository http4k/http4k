/*
 * Copyright (c) 2025-present http4k Ltd. All rights reserved.
 * Licensed under the http4k Commercial License: https://http4k.org/commercial-license
 */
package org.http4k.ai.mcp.stateless.server.capability

import dev.forkhandles.result4k.get
import dev.forkhandles.result4k.map
import dev.forkhandles.result4k.mapFailure
import dev.forkhandles.result4k.resultFrom
import org.http4k.ai.mcp.stateless.Client
import org.http4k.ai.mcp.stateless.ToolFilter
import org.http4k.ai.mcp.stateless.ToolHandler
import org.http4k.ai.mcp.stateless.ToolRequest
import org.http4k.ai.mcp.stateless.ToolResponse.Error
import org.http4k.ai.mcp.stateless.ToolResponse.InputRequired
import org.http4k.ai.mcp.stateless.ToolResponse.Ok
import org.http4k.ai.mcp.stateless.ToolResponse.Task
import org.http4k.ai.mcp.stateless.model.Meta
import org.http4k.ai.mcp.stateless.model.ResultType
import org.http4k.ai.mcp.stateless.model.Tool
import org.http4k.ai.mcp.stateless.model.mirroredHeaderArgs
import org.http4k.ai.mcp.stateless.protocol.McpException
import org.http4k.ai.mcp.stateless.protocol.decodeMcpHeaderValue
import org.http4k.ai.mcp.stateless.protocol.messages.HeaderMismatchError
import org.http4k.ai.mcp.stateless.protocol.messages.McpTool
import org.http4k.ai.mcp.stateless.then
import org.http4k.ai.mcp.stateless.util.McpJson
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
import org.http4k.format.unwrap
import org.http4k.jsonrpc.ErrorMessage
import org.http4k.jsonrpc.ErrorMessage.Companion.InvalidParams
import org.http4k.lens.LensFailure

data class ToolCapability(internal val tool: Tool, internal val handler: ToolHandler) : ServerCapability, ToolHandler {
    override val name = tool.name.value

    @Suppress("UNCHECKED_CAST")
    fun toTool() = McpTool(
        tool.name, tool.description,
        tool.title,
        tool.toSchema().unwrap() as Map<String, Any>,
        tool.output?.toSchema()?.let { it.unwrap() as Map<String, Any> },
        tool.annotations,
        tool.icons,
        tool.meta ?: Meta.default
    )

    @Suppress("UNCHECKED_CAST")
    fun call(mcp: McpTool.Call.Request.Params, client: Client, http: Request) = run {
        tool.validateMirroredHeaders(mcp.arguments, http)

        resultFrom {
            ToolRequest(
                mcp.arguments.coerceIntoRawTypes(), mcp._meta, client, http,
                mcp.inputResponses.toElicitationResponses(),
                mcp.requestState
            )
        }
            .mapFailure { throw McpException(InvalidParams) }
            .map {
                try {
                    this(it)
                } catch (e: LensFailure) {
                    throw McpException(InvalidParams, e)
                } catch (e: McpException) {
                    throw e
                } catch (e: Exception) {
                    throw McpException(ErrorMessage.InternalError, e)
                }
            }
            .get()
            .let {
                when (it) {
                    is Ok -> McpTool.Call.Response.Result(
                        content = it.content,
                        structuredContent = it.structuredContent,
                        isError = false,
                        _meta = it.meta
                    )

                    is Error -> McpTool.Call.Response.Result(
                        content = it.content,
                        structuredContent = it.structuredContent,
                        isError = true,
                        _meta = it.meta
                    )

                    is InputRequired -> McpTool.Call.Response.Result(
                        resultType = ResultType.input_required,
                        inputRequests = it.inputRequests.toWireRequests(mcp._meta.clientCapabilities()),
                        requestState = it.requestState,
                        _meta = it.meta
                    )

                    is Task -> McpTool.Call.Response.Result(
                        taskId = it.task.taskId,
                        status = it.task.status,
                        statusMessage = it.task.statusMessage,
                        createdAt = it.task.createdAt,
                        lastUpdatedAt = it.task.lastUpdatedAt,
                        ttlMs = it.task.ttlMs,
                        pollIntervalMs = it.task.pollIntervalMs,
                        resultType = ResultType.task,
                        _meta = it.meta
                    )
                }
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

fun ToolFilter.then(capability: ToolCapability) = ToolCapability(capability.tool, then(capability))

private fun Tool.validateMirroredHeaders(arguments: Map<String, Any>, http: Request) =
    mirroredHeaderArgs().forEach { (arg, header) ->
        val expected = arguments[arg]?.takeIf { it != MoshiNull }?.let(::asHeaderComparable)

        when (val actual = http.header("Mcp-Param-$header")?.let(::decodeMcpHeaderValue)) {
            expected -> Unit

            else -> throw McpException(
                HeaderMismatchError(
                    when {
                        expected == null -> "Mcp-Param-$header sent but $arg is absent from the body"
                        actual == null -> "Mcp-Param-$header is missing or malformed for $arg"
                        else -> "Mcp-Param-$header does not match the body value of $arg"
                    }
                )
            )
        }
    }

private fun asHeaderComparable(value: Any) = when (value) {
    is MoshiString -> value.value
    is MoshiNode -> value.unwrap().toString()
    else -> value.toString()
}
