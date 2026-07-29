/*
 * Copyright (c) 2025-present http4k Ltd. All rights reserved.
 * Licensed under the http4k Commercial License: https://http4k.org/commercial-license
 */
package org.http4k.ai.mcp.server.protocol

import org.http4k.ai.mcp.protocol.messages.HeaderMismatchError
import org.http4k.ai.mcp.protocol.messages.McpJsonRpcErrorResponse
import org.http4k.lens.Header
import org.http4k.lens.MCP_METHOD

// Stateless: the Mcp-Method mirror header is REQUIRED on every request and must match the body method.
fun ValidateMcpMethodHeader() = McpFilter { next ->
    { mcp ->
        when (val mcpMethod = Header.MCP_METHOD(mcp.http)) {
            mcp.message.method -> next(mcp)
            null -> McpResponse.Ok(
                McpJsonRpcErrorResponse(mcp.message.id, HeaderMismatchError("Mcp-Method header is required"))
            )

            else -> McpResponse.Ok(
                McpJsonRpcErrorResponse(
                    mcp.message.id,
                    HeaderMismatchError("Mcp-Method header value '${mcpMethod.value}' does not match body value '${mcp.message.method.value}'")
                )
            )
        }
    }
}
