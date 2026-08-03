/*
 * Copyright (c) 2025-present http4k Ltd. All rights reserved.
 * Licensed under the http4k Commercial License: https://http4k.org/commercial-license
 */
package org.http4k.ai.mcp.server.protocol

import org.http4k.ai.mcp.model.Meta
import org.http4k.ai.mcp.protocol.ServerMetaData
import org.http4k.ai.mcp.protocol.messages.HeaderMismatchError
import org.http4k.ai.mcp.protocol.messages.McpJsonRpcErrorResponse
import org.http4k.ai.mcp.protocol.messages.McpJsonRpcRequest
import org.http4k.ai.mcp.protocol.messages.UnsupportedProtocolVersionError
import org.http4k.ai.mcp.protocol.messages.mirroredName
import org.http4k.core.Request
import org.http4k.jsonrpc.ErrorMessage.Companion.InvalidParams
import org.http4k.lens.MetaKey
import org.http4k.lens.clientCapabilities
import org.http4k.lens.protocolVersion

internal fun McpJsonRpcRequest.validate(http: Request, serverMetaData: ServerMetaData): McpJsonRpcErrorResponse? {
    val meta = meta()

    val version = MetaKey.protocolVersion().toLens()(meta)

    return when {
        version == null || MetaKey.clientCapabilities().toLens()(meta) == null ->
            McpJsonRpcErrorResponse(id, InvalidParams)

        http.header("mcp-protocol-version")?.trim()?.let { it != version.value } == true ->
            McpJsonRpcErrorResponse(
                id,
                HeaderMismatchError("MCP-Protocol-Version header does not match _meta protocolVersion")
            )

        version !in serverMetaData.protocolVersions ->
            McpJsonRpcErrorResponse(
                id,
                UnsupportedProtocolVersionError(version, serverMetaData.protocolVersions)
            )

        http.header("mcp-method")?.trim() != method.value ->
            McpJsonRpcErrorResponse(id, HeaderMismatchError("Mcp-Method header does not match body method"))

        mirroredName()?.let { it != http.header("mcp-name")?.trim() } == true ->
            McpJsonRpcErrorResponse(id, HeaderMismatchError("Mcp-Name header does not match body target"))

        else -> null
    }
}

internal fun McpJsonRpcRequest.meta(): Meta = params?._meta ?: Meta.default

