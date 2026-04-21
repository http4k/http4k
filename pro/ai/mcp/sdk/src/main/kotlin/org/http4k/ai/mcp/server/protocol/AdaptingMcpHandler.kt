/*
 * Copyright (c) 2025-present http4k Ltd. All rights reserved.
 * Licensed under the http4k Commercial License: https://http4k.org/commercial-license
 */
package org.http4k.ai.mcp.server.protocol

import org.http4k.ai.mcp.Client
import org.http4k.ai.mcp.protocol.McpException
import org.http4k.ai.mcp.protocol.messages.ClientMessage
import org.http4k.ai.mcp.protocol.messages.ServerMessage.Response
import org.http4k.ai.mcp.protocol.messages.fromJsonRpc
import org.http4k.ai.mcp.protocol.messages.toJsonRpc
import org.http4k.ai.mcp.util.McpNodeType
import org.http4k.jsonrpc.ErrorMessage
import org.http4k.jsonrpc.ErrorMessage.Companion.InvalidRequest
import org.http4k.jsonrpc.JsonRpcRequest
import org.http4k.jsonrpc.JsonRpcResult
import kotlin.reflect.KClass

class AdaptingMcpHandler(private val onError: (Throwable) -> Unit) {
    operator fun <IN : ClientMessage.Request> invoke(clazz: KClass<IN>, fn: (IN, Client) -> Response, client: Client): McpHandler =
        { req: McpRequest ->
            when (val jsonRpc = req.json) {
                is JsonRpcRequest<McpNodeType> -> McpResponse.Ok(
                    jsonRpc.runCatching { jsonRpc.fromJsonRpc(clazz) }
                        .mapCatching { fn(it, client) }
                        .map { it.toJsonRpc(jsonRpc.id) }
                        .recover {
                            when (it) {
                                is McpException -> it.error.toJsonRpc(jsonRpc.id)
                                else -> {
                                    onError(it)
                                    ErrorMessage.InternalError.toJsonRpc(jsonRpc.id)
                                }
                            }
                        }
                        .getOrElse { InvalidRequest.toJsonRpc(jsonRpc.id) }
                )

                // TODO - make this actually do something!
                is JsonRpcResult<*> -> McpResponse.Ok(InvalidRequest.toJsonRpc(null))
            }

    }
}
