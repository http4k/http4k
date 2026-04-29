/*
 * Copyright (c) 2025-present http4k Ltd. All rights reserved.
 * Licensed under the http4k Commercial License: https://http4k.org/commercial-license
 */
package org.http4k.ai.mcp.server.protocol

import org.http4k.ai.mcp.protocol.ProtocolVersion.Companion.DRAFT
import org.http4k.ai.mcp.protocol.messages.HeaderMismatchError
import org.http4k.ai.mcp.protocol.messages.McpJsonRpcErrorResponse
import org.http4k.lens.Header
import org.http4k.lens.MCP_METHOD

fun ValidateMcpMethodHeader(clientTracking: Map<Session, ClientTracking>) = McpFilter { next ->
    { mcp ->
        val tracking = clientTracking[mcp.session]
        val mcpMethod = Header.MCP_METHOD(mcp.http)

        when {
            tracking != null &&
                tracking.protocolVersion >= DRAFT &&
                mcpMethod != null && mcpMethod != mcp.message.method ->
                McpResponse.Ok(
                    McpJsonRpcErrorResponse(
                        mcp.message.id,
                        HeaderMismatchError("Mcp-Method header value '${mcpMethod.value}' does not match body value '${mcp.message.method.value}'")
                    )
                )

            else -> next(mcp)
        }
    }
}
