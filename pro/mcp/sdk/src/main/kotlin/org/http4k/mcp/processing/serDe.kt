package org.http4k.mcp.processing

import org.http4k.format.renderError
import org.http4k.format.renderRequest
import org.http4k.format.renderResult
import org.http4k.jsonrpc.ErrorMessage
import org.http4k.jsonrpc.JsonRpcRequest
import org.http4k.jsonrpc.JsonRpcResult
import org.http4k.mcp.protocol.messages.HasMethod
import org.http4k.mcp.protocol.messages.McpNotification
import org.http4k.mcp.protocol.messages.McpRequest
import org.http4k.mcp.protocol.messages.McpResponse
import org.http4k.mcp.util.McpJson
import org.http4k.mcp.util.McpNodeType

inline fun <reified OUT : Any> McpJson.fromJsonRpc(input: JsonRpcRequest<McpNodeType>): OUT =
    asA<OUT>(asFormatString(input.params ?: obj()))

inline fun <reified OUT : Any> McpJson.fromJsonRpc(input: JsonRpcResult<McpNodeType>): OUT =
    asA<OUT>(compact(input.result ?: nullNode()))

fun McpJson.toJsonRpc(method: HasMethod, input: McpRequest, id: McpNodeType) =
    renderRequest(method.Method.value, asJsonObject(input), id)

fun McpJson.toJsonRpc(input: McpResponse, id: McpNodeType?) =
    renderResult(asJsonObject(input), id ?: nullNode())

fun McpJson.toJsonRpc(method: HasMethod, input: McpNotification) =
    renderRequest(method.Method.value, asJsonObject(input), nullNode())

fun McpJson.toJsonRpc(errorMessage: ErrorMessage, id: McpNodeType?) = renderError(errorMessage, id)
