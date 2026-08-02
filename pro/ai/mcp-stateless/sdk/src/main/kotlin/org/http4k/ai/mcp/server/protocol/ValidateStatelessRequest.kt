/*
 * Copyright (c) 2025-present http4k Ltd. All rights reserved.
 * Licensed under the http4k Commercial License: https://http4k.org/commercial-license
 */
package org.http4k.ai.mcp.server.protocol

import org.http4k.ai.mcp.protocol.ProtocolVersion
import org.http4k.ai.mcp.protocol.messages.HeaderMismatchError
import org.http4k.ai.mcp.protocol.messages.McpJsonRpcErrorResponse
import org.http4k.ai.mcp.protocol.messages.UnsupportedProtocolVersionError
import org.http4k.ai.mcp.util.McpJson
import org.http4k.core.Request
import org.http4k.format.MoshiObject
import org.http4k.jsonrpc.ErrorMessage.Companion.InvalidParams

private const val PROTOCOL_VERSION_KEY = "io.modelcontextprotocol/protocolVersion"
private const val CLIENT_CAPABILITIES_KEY = "io.modelcontextprotocol/clientCapabilities"

/**
 * Stateless per-request validation (2026-07-28), run BEFORE the streaming/non-streaming split so it can
 * reject with a JSON 4xx regardless of `Accept`. Every request self-describes via reserved `params._meta`:
 * - missing `_meta` / `protocolVersion` / `clientCapabilities` -> `-32602`
 * - `MCP-Protocol-Version` header present and != `_meta.protocolVersion` -> `-32020` (checked before support,
 *    so a mismatch on an unsupported version still reports the mismatch)
 * - `_meta.protocolVersion` not supported -> `-32022`
 * `clientInfo` is optional (a request omitting it MUST still succeed). Returns null when the request is valid.
 * The version is read as a raw string (not the typed lens) so an unknown-but-present version reports as
 * unsupported (`-32022`), not missing (`-32602`).
 */
internal fun validateStatelessRequest(
    body: String,
    http: Request,
    supported: Set<ProtocolVersion>
): McpJsonRpcErrorResponse? {
    val meta = body.metaNode()
    val id = body.requestId()
    val versionNode = meta?.get(PROTOCOL_VERSION_KEY)

    return when {
        meta == null || versionNode == null || meta[CLIENT_CAPABILITIES_KEY] == null ->
            McpJsonRpcErrorResponse(id, InvalidParams)

        else -> {
            val version = ProtocolVersion.of(McpJson.text(versionNode))
            when {
                http.headerMismatch(version) -> McpJsonRpcErrorResponse(
                    id, HeaderMismatchError("MCP-Protocol-Version header does not match _meta protocolVersion")
                )

                version !in supported ->
                    McpJsonRpcErrorResponse(id, UnsupportedProtocolVersionError(version, supported.toList()))

                else -> null
            }
        }
    }
}

private fun Request.headerMismatch(metaVersion: ProtocolVersion) =
    header("mcp-protocol-version")?.trim()?.let { it != metaVersion.value } ?: false

private fun String.metaNode(): MoshiObject? =
    ((parsed() as? MoshiObject)?.get("params") as? MoshiObject)?.get("_meta") as? MoshiObject

private fun String.requestId(): Any? = (parsed() as? MoshiObject)?.get("id")

private fun String.parsed() = runCatching { McpJson.parse(this) }.getOrNull()
