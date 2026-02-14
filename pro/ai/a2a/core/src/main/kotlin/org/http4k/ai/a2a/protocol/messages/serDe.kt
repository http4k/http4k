package org.http4k.ai.a2a.protocol.messages

import org.http4k.ai.a2a.util.A2AJson
import org.http4k.ai.a2a.util.A2ANodeType
import org.http4k.format.renderError
import org.http4k.format.renderRequest
import org.http4k.format.renderResult
import org.http4k.jsonrpc.ErrorMessage
import org.http4k.jsonrpc.JsonRpcRequest
import org.http4k.jsonrpc.JsonRpcResult

inline fun <reified OUT : Any> JsonRpcRequest<A2ANodeType>.fromJsonRpc(): OUT =
    A2AJson.asA<OUT>(A2AJson.asFormatString(params ?: A2AJson.obj()))

inline fun <reified OUT : Any> JsonRpcResult<A2ANodeType>.fromJsonRpc(): OUT =
    A2AJson.asA<OUT>(A2AJson.compact(result ?: A2AJson.nullNode()))

fun A2ARequest.toJsonRpc(method: A2ARpc, id: A2ANodeType) =
    A2AJson.renderRequest(method.Method.value, A2AJson.asJsonObject(this), id)

fun A2AResponse.toJsonRpc(id: A2ANodeType?) =
    A2AJson.renderResult(A2AJson.asJsonObject(this), id ?: A2AJson.nullNode())

fun ErrorMessage.toJsonRpc(id: A2ANodeType?) = A2AJson.renderError(this, id)
