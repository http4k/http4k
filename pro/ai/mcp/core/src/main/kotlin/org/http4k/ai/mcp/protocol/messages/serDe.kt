package org.http4k.ai.mcp.protocol.messages

import org.http4k.ai.mcp.util.McpJson
import org.http4k.ai.mcp.util.McpNodeType
import org.http4k.format.renderError
import org.http4k.format.renderRequest
import org.http4k.format.renderResult
import org.http4k.jsonrpc.ErrorMessage
import org.http4k.jsonrpc.JsonRpcRequest
import org.http4k.jsonrpc.JsonRpcResult
import kotlin.reflect.KClass

fun <OUT : Any> JsonRpcRequest<McpNodeType>.fromJsonRpc(clazz: KClass<OUT>): OUT =
    McpJson.asA(McpJson.asFormatString(params ?: McpJson.obj()), clazz)

fun <OUT : Any> JsonRpcResult<McpNodeType>.fromJsonRpc(clazz: KClass<OUT>): OUT =
    McpJson.asA(McpJson.compact(result ?: McpJson.nullNode()), clazz)

fun McpRequest.toJsonRpc(method: McpRpc, id: McpNodeType) =
    McpJson.renderRequest(method.Method.value, McpJson.asJsonObject(this), id)

fun McpResponse.toJsonRpc(id: McpNodeType?) =
    McpJson.renderResult(McpJson.asJsonObject(this), id ?: McpJson.nullNode())

fun McpNotification.toJsonRpc(method: McpRpc) =
    McpJson.renderRequest(method.Method.value, McpJson.asJsonObject(this), McpJson.nullNode())

fun ErrorMessage.toJsonRpc(id: McpNodeType?) = McpJson.renderError(this, id)
