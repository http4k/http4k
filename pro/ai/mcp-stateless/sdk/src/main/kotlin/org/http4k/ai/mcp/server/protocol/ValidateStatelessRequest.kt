/*
 * Copyright (c) 2025-present http4k Ltd. All rights reserved.
 * Licensed under the http4k Commercial License: https://http4k.org/commercial-license
 */
package org.http4k.ai.mcp.server.protocol

import org.http4k.ai.mcp.model.LogLevel
import org.http4k.ai.mcp.model.Meta
import org.http4k.ai.mcp.protocol.ProtocolVersion
import org.http4k.ai.mcp.protocol.messages.HeaderMismatchError
import org.http4k.ai.mcp.protocol.messages.McpJsonRpcErrorResponse
import org.http4k.ai.mcp.protocol.messages.McpJsonRpcRequest
import org.http4k.ai.mcp.protocol.messages.UnsupportedProtocolVersionError
import org.http4k.core.Request
import org.http4k.jsonrpc.ErrorMessage.Companion.InvalidParams
import org.http4k.lens.MetaKey
import org.http4k.lens.clientCapabilities
import org.http4k.lens.logLevel
import org.http4k.lens.protocolVersion

// The reserved request `_meta`, read straight off the already-parsed typed message — never re-parsing the body.
internal fun McpJsonRpcRequest.meta(): Meta = params?._meta ?: Meta.default

internal fun McpJsonRpcRequest.logLevel(): LogLevel? = MetaKey.logLevel().toLens()(meta())

/**
 * Stateless per-request validation (2026-07-28), off the typed message so it can run before the
 * streaming/non-streaming split and reject with a JSON 4xx regardless of `Accept`:
 * - missing `_meta.protocolVersion` / `_meta.clientCapabilities` -> `-32602`
 * - `MCP-Protocol-Version` header present and != `_meta.protocolVersion` -> `-32020` (before support, so a
 *    mismatch on an unsupported version still reports the mismatch)
 * - `_meta.protocolVersion` not supported -> `-32022`
 * `clientInfo` is optional; the header value is OWS-trimmed (RFC 9110). Returns null when the request is valid.
 */
internal fun validateStatelessRequest(
    message: McpJsonRpcRequest,
    http: Request,
    supported: Set<ProtocolVersion>
): McpJsonRpcErrorResponse? {
    val meta = message.meta()
    val id = message.id
    val version = MetaKey.protocolVersion().toLens()(meta)

    return when {
        version == null || MetaKey.clientCapabilities().toLens()(meta) == null ->
            McpJsonRpcErrorResponse(id, InvalidParams)

        http.header("mcp-protocol-version")?.trim()?.let { it != version.value } == true ->
            McpJsonRpcErrorResponse(id, HeaderMismatchError("MCP-Protocol-Version header does not match _meta protocolVersion"))

        version !in supported ->
            McpJsonRpcErrorResponse(id, UnsupportedProtocolVersionError(version, supported.toList()))

        else -> null
    }
}
