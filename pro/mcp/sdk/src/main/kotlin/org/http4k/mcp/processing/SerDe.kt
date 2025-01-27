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

object SerDe {
    inline operator fun <reified OUT : Any> invoke(input: JsonRpcRequest<McpNodeType>): OUT = with(McpJson) {
        asA<OUT>(asFormatString(input.params ?: obj()))
    }

    inline operator fun <reified OUT : Any> invoke(input: JsonRpcResult<McpNodeType>): OUT = with(McpJson) {
        asA<OUT>(compact(input.result ?: nullNode()))
    }

    operator fun invoke(method: HasMethod, input: McpRequest, id: McpNodeType) = with(McpJson) {
        renderRequest(method.Method.value, asJsonObject(input), id)
    }

    operator fun invoke(input: McpResponse, id: McpNodeType?) = with(McpJson) {
        renderResult(asJsonObject(input), id ?: nullNode())
    }

    operator fun invoke(method: HasMethod, input: McpNotification) = with(McpJson) {
        renderRequest(method.Method.value, asJsonObject(input), nullNode())
    }

    operator fun invoke(errorMessage: ErrorMessage, id: McpNodeType?) = with(McpJson) {
        renderError(errorMessage, id)
    }
}
