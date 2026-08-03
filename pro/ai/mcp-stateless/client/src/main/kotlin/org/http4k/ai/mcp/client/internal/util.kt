/*
 * Copyright (c) 2025-present http4k Ltd. All rights reserved.
 * Licensed under the http4k Commercial License: https://http4k.org/commercial-license
 */
package org.http4k.ai.mcp.client.internal

import dev.forkhandles.result4k.Failure
import dev.forkhandles.result4k.Success
import dev.forkhandles.result4k.flatMapFailure
import dev.forkhandles.result4k.resultFrom
import org.http4k.ai.mcp.CompletionResponse
import org.http4k.ai.mcp.ElicitationRequest
import org.http4k.ai.mcp.McpError
import org.http4k.ai.mcp.McpError.Internal
import org.http4k.ai.mcp.McpError.Protocol
import org.http4k.ai.mcp.PromptResponse
import org.http4k.ai.mcp.ResourceResponse
import org.http4k.ai.mcp.ToolResponse
import org.http4k.ai.mcp.ToolResponse.Error
import org.http4k.ai.mcp.ToolResponse.InputRequired
import org.http4k.ai.mcp.ToolResponse.Ok
import org.http4k.ai.mcp.model.Meta
import org.http4k.ai.mcp.model.ResultType
import org.http4k.ai.mcp.protocol.ClientCapabilities
import org.http4k.ai.mcp.protocol.ProtocolVersion
import org.http4k.ai.mcp.protocol.VersionedMcpEntity
import org.http4k.ai.mcp.protocol.messages.DomainError
import org.http4k.ai.mcp.protocol.messages.McpElicitation
import org.http4k.ai.mcp.protocol.messages.McpJsonRpcRequest
import org.http4k.ai.mcp.protocol.messages.McpTool
import org.http4k.ai.mcp.protocol.messages.mirroredName
import org.http4k.ai.mcp.util.McpJson
import org.http4k.ai.mcp.util.McpNodeType
import org.http4k.core.ContentType.Companion.APPLICATION_JSON
import org.http4k.core.Method.POST
import org.http4k.core.Request
import org.http4k.core.Uri
import org.http4k.core.with
import org.http4k.format.MoshiNode
import org.http4k.format.MoshiObject
import org.http4k.jsonrpc.ErrorMessage
import org.http4k.lens.Header
import org.http4k.lens.MCP_METHOD
import org.http4k.lens.MCP_NAME
import org.http4k.lens.MCP_PROTOCOL_VERSION
import org.http4k.lens.MetaKey
import org.http4k.lens.clientCapabilities
import org.http4k.lens.clientInfo
import org.http4k.lens.contentType
import org.http4k.lens.protocolVersion
import org.http4k.lens.serverInfo
import se.ansman.kotshi.JsonSerializable

internal fun McpJsonRpcRequest.toHttpRequest(
    protocolVersion: ProtocolVersion,
    endpoint: Uri,
    clientInfo: VersionedMcpEntity,
    capabilities: ClientCapabilities
) = Request(POST, endpoint)
    .contentType(APPLICATION_JSON)
    .with(Header.MCP_PROTOCOL_VERSION of protocolVersion)
    .with(Header.MCP_METHOD of method)
    .let { req -> mirroredName()?.let { req.with(Header.MCP_NAME of it) } ?: req }
    .body(McpJson.compact(McpJson.asJsonObject(this).withClientMeta(protocolVersion, clientInfo, capabilities)))

private fun MoshiNode.withClientMeta(
    protocolVersion: ProtocolVersion,
    clientInfo: VersionedMcpEntity,
    capabilities: ClientCapabilities
): MoshiNode {
    if (this !is MoshiObject) return this
    val params = attributes["params"] as? MoshiObject ?: return this
    val existingMeta = params.attributes["_meta"] as? MoshiObject ?: MoshiObject()
    val meta = MetaKey.protocolVersion().toLens()(
        protocolVersion,
        MetaKey.clientInfo().toLens()(
            clientInfo,
            MetaKey.clientCapabilities().toLens()(capabilities, Meta(existingMeta))
        )
    ).node
    val newParams = MoshiObject((params.attributes + ("_meta" to meta)).toMutableMap())
    return MoshiObject((attributes + ("params" to newParams)).toMutableMap())
}

internal inline fun <reified T : Any> McpNodeType.asOrFailure() = with(McpJson) {
    val obj = this@asOrFailure as MoshiObject
    val error = obj["error"]
    when {
        error != null -> Failure(Protocol(
            when (error) {
                is MoshiObject -> (error["code"]?.let { integer(it).toInt() } ?: -1).let { code ->
                    val message = error["message"]?.let { text(it) } ?: "Unknown error"
                    error["data"]?.let { ErrorMessageWithData(code, message, it) } ?: ErrorMessage(code, message)
                }

                else -> ErrorMessageWithData(-1, error.toString())
            }
        ))

        else -> resultFrom { asA<T>(compact((obj["result"] ?: nullNode()).stripProtocolMeta())) }
            .flatMapFailure { Failure(Internal(it)) }
    }
}

internal fun McpNodeType.serverInfoOrNull(): VersionedMcpEntity? {
    val result = (this as? MoshiObject)?.get("result") as? MoshiObject ?: return null
    val meta = result.attributes["_meta"] as? MoshiObject ?: return null
    return MetaKey.serverInfo().toLens()(Meta(meta))
}

private fun McpNodeType.stripProtocolMeta(): McpNodeType {
    if (this !is MoshiObject) return this
    val meta = attributes["_meta"] as? MoshiObject ?: return this
    val cleaned = meta.attributes.filterKeys { !it.startsWith("io.modelcontextprotocol/") }.toMutableMap()
    return MoshiObject((attributes + ("_meta" to MoshiObject(cleaned))).toMutableMap())
}

@JsonSerializable
data class ErrorMessageWithData(override val code: Int, override val message: String, val data: McpNodeType? = null) :
    ErrorMessage(code, message)

internal fun McpElicitation.Create.toElicitationRequest(): ElicitationRequest = when (val p = params) {
    is McpElicitation.Create.Params.Form -> ElicitationRequest.Form(p.message, p.requestedSchema)
    is McpElicitation.Create.Params.Url -> ElicitationRequest.Url(p.message, p.url)
}

fun toToolResponseOrError(response: McpTool.Call.Response.Result): ToolResponse = when {
    response.resultType == ResultType.input_required -> InputRequired(
        response.inputRequests.orEmpty().mapValues { it.value.toElicitationRequest() },
        response.requestState, response._meta
    )

    response.isError == true -> Error(response.content, response.structuredContent, response._meta)

    else -> Ok(response.content, response.structuredContent, response._meta)
}

fun toResourceErrorOrFailure(mcpError: McpError) = when (mcpError) {
    is Protocol if mcpError.error.code == DomainError.CODE ->
        Success(ResourceResponse.Error(mcpError.error.message))

    else -> Failure(mcpError)
}

fun toPromptErrorOrFailure(mcpError: McpError) =
    when (mcpError) {
        is Protocol if mcpError.error.code == DomainError.CODE -> Success(PromptResponse.Error(mcpError.error.message))
        else -> Failure(mcpError)
    }

fun toCompletionErrorOrFailure(mcpError: McpError) =
    when (mcpError) {
        is Protocol if mcpError.error.code == DomainError.CODE ->
            Success(CompletionResponse.Error(mcpError.error.message))

        else -> Failure(mcpError)
    }
