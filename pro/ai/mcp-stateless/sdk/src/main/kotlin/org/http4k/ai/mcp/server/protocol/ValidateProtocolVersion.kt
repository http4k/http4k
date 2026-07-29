/*
 * Copyright (c) 2025-present http4k Ltd. All rights reserved.
 * Licensed under the http4k Commercial License: https://http4k.org/commercial-license
 */
package org.http4k.ai.mcp.server.protocol

import org.http4k.ai.mcp.protocol.ProtocolVersion
import org.http4k.ai.mcp.protocol.messages.McpJsonRpcErrorResponse
import org.http4k.ai.mcp.protocol.messages.UnsupportedProtocolVersionError
import org.http4k.lens.Header
import org.http4k.lens.MCP_PROTOCOL_VERSION

/**
 * Stateless per-request protocol-version check: the version rides the `MCP-Protocol-Version` header
 * (mirroring `_meta.protocolVersion`), so this stays fully typed — no JSON node surgery. Unsupported
 * versions are rejected with `UnsupportedProtocolVersionError` (-32022) carrying the supported set.
 */
fun ValidateProtocolVersion(supported: Set<ProtocolVersion>) = McpFilter { next ->
    { mcp ->
        when (val version = Header.MCP_PROTOCOL_VERSION(mcp.http)) {
            in supported -> next(mcp)
            else -> McpResponse.Ok(
                McpJsonRpcErrorResponse(mcp.message.id, UnsupportedProtocolVersionError(version, supported.toList()))
            )
        }
    }
}
