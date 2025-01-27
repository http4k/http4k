package org.http4k.mcp.protocol.messages

import org.http4k.format.renderError
import org.http4k.format.renderRequest
import org.http4k.format.renderResult
import org.http4k.jsonrpc.ErrorMessage
import org.http4k.jsonrpc.JsonRpcRequest
import org.http4k.jsonrpc.JsonRpcResult
import org.http4k.mcp.util.McpJson
import org.http4k.mcp.util.McpNodeType

inline fun <reified OUT : Any> JsonRpcRequest<McpNodeType>.fromJsonRpc(): OUT =
    McpJson.asA<OUT>(McpJson.asFormatString(params ?: McpJson.obj()))

inline fun <reified OUT : Any> JsonRpcResult<McpNodeType>.fromJsonRpc(): OUT =
    McpJson.asA<OUT>(McpJson.compact(result ?: McpJson.nullNode()))

fun McpRequest.toJsonRpc(method: HasMethod, id: McpNodeType) =
    McpJson.renderRequest(method.Method.value, McpJson.asJsonObject(this), id)

fun McpResponse.toJsonRpc(id: McpNodeType?) =
    McpJson.renderResult(McpJson.asJsonObject(this), id ?: McpJson.nullNode())

fun McpNotification.toJsonRpc(method: HasMethod) =
    McpJson.renderRequest(method.Method.value, McpJson.asJsonObject(this), McpJson.nullNode())

fun ErrorMessage.toJsonRpc(id: McpNodeType?) = McpJson.renderError(this, id)
