package org.http4k.ai.mcp.protocol.messages

import org.http4k.format.renderError
import org.http4k.format.renderRequest
import org.http4k.format.renderResult
import org.http4k.jsonrpc.ErrorMessage
import org.http4k.jsonrpc.JsonRpcRequest
import org.http4k.jsonrpc.JsonRpcResult
import org.http4k.ai.mcp.util.McpJson
import org.http4k.ai.mcp.util.McpNodeType

inline fun <reified OUT : Any> JsonRpcRequest<McpNodeType>.fromJsonRpc(): OUT =
    McpJson.asA<OUT>(McpJson.asFormatString(params ?: McpJson.obj()))

inline fun <reified OUT : Any> JsonRpcResult<McpNodeType>.fromJsonRpc(): OUT =
    McpJson.asA<OUT>(McpJson.compact(result ?: McpJson.nullNode()))

fun org.http4k.ai.mcp.protocol.messages.McpRequest.toJsonRpc(method: org.http4k.ai.mcp.protocol.messages.McpRpc, id: McpNodeType) =
    McpJson.renderRequest(method.Method.value, McpJson.asJsonObject(this), id)

fun org.http4k.ai.mcp.protocol.messages.McpResponse.toJsonRpc(id: McpNodeType?) =
    McpJson.renderResult(McpJson.asJsonObject(this), id ?: McpJson.nullNode())

fun org.http4k.ai.mcp.protocol.messages.McpNotification.toJsonRpc(method: org.http4k.ai.mcp.protocol.messages.McpRpc) =
    McpJson.renderRequest(method.Method.value, McpJson.asJsonObject(this), McpJson.nullNode())

fun ErrorMessage.toJsonRpc(id: McpNodeType?) = McpJson.renderError(this, id)
