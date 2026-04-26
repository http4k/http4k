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
import org.http4k.jsonrpc.ErrorMessage
import org.http4k.jsonrpc.ErrorMessage.Companion.InvalidRequest
import kotlin.reflect.KClass


class AdaptingMcpHandler(private val onError: (Throwable) -> Unit) {
    operator fun <IN : ClientMessage.Request> invoke(clazz: KClass<IN>, fn: (IN, Client) -> Response, client: Client) =
        { req: McpRequest ->
            McpResponse(
                req.json.runCatching { req.json.fromJsonRpc(clazz) }
                    .mapCatching { fn(it, client) }
                    .map { it.toJsonRpc(req.json.id) }
                    .recover {
                        when (it) {
                            is McpException -> it.error.toJsonRpc(req.json.id)
                            else -> {
                                onError(it)
                                ErrorMessage.InternalError.toJsonRpc(req.json.id)
                            }
                        }
                    }
                    .getOrElse { InvalidRequest.toJsonRpc(req.json.id) }
            )
    }
}
