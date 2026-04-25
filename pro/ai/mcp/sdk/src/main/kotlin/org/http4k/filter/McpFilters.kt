/*
 * Copyright (c) 2025-present http4k Ltd. All rights reserved.
 * Licensed under the http4k Commercial License: https://http4k.org/commercial-license
 */
package org.http4k.filter

import org.http4k.ai.mcp.protocol.McpException
import org.http4k.ai.mcp.protocol.messages.McpJsonRpcErrorResponse
import org.http4k.ai.mcp.server.protocol.McpFilter
import org.http4k.ai.mcp.server.protocol.McpResponse.Ok
import org.http4k.jsonrpc.ErrorMessage

object McpFilters {
    fun CatchAll(onError: (Throwable) -> Unit): McpFilter = McpFilter { next ->
        {
            runCatching { next(it) }
                .getOrElse { e ->
                    Ok(
                        McpJsonRpcErrorResponse(
                            it.message.id,
                            when (e) {
                                is McpException -> e.error
                                else -> {
                                    onError(e)
                                    ErrorMessage.InternalError
                                }
                            }
                        )
                    )
                }
        }
    }
}


