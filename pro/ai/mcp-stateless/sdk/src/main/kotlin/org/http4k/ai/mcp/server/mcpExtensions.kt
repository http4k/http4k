/*
 * Copyright (c) 2025-present http4k Ltd. All rights reserved.
 * Licensed under the http4k Commercial License: https://http4k.org/commercial-license
 */
package org.http4k.ai.mcp.server

import org.http4k.ai.mcp.protocol.ServerMetaData
import org.http4k.ai.mcp.protocol.messages.HeaderMismatchError
import org.http4k.ai.mcp.protocol.messages.McpJsonRpcErrorResponse
import org.http4k.ai.mcp.protocol.messages.McpJsonRpcMessage
import org.http4k.ai.mcp.protocol.messages.MissingRequiredClientCapabilityError
import org.http4k.ai.mcp.protocol.messages.UnsupportedProtocolVersionError
import org.http4k.ai.mcp.server.capability.ServerCapability
import org.http4k.ai.mcp.server.protocol.McpResponse
import org.http4k.ai.mcp.server.protocol.McpResponse.Accepted
import org.http4k.ai.mcp.server.protocol.McpResponse.Ok
import org.http4k.ai.mcp.server.security.NoMcpSecurity
import org.http4k.ai.mcp.util.McpJson
import org.http4k.core.ContentType.Companion.APPLICATION_JSON
import org.http4k.core.Response
import org.http4k.core.Status
import org.http4k.core.Status.Companion.ACCEPTED
import org.http4k.core.Status.Companion.BAD_REQUEST
import org.http4k.core.Status.Companion.NOT_FOUND
import org.http4k.core.Status.Companion.OK
import org.http4k.core.with
import org.http4k.format.MoshiObject
import org.http4k.jsonrpc.ErrorMessage.Companion.InvalidParams
import org.http4k.jsonrpc.ErrorMessage.Companion.MethodNotFound
import org.http4k.lens.Header
import org.http4k.routing.mcp
import org.http4k.server.PolyServerConfig
import org.http4k.server.asServer

/**
 * Convenience function to create a server from a single capability
 */
fun ServerCapability.asServer(cfg: PolyServerConfig, name: String = "http4k-mcp") = asMcp(name).asServer(cfg)

/**
 * Convenience function to create a server from a multiple capabilities
 */
fun Iterable<ServerCapability>.asServer(config: PolyServerConfig, name: String = "http4k-mcp") =
    asMcp(name).asServer(config)

/**
 * Convenience function to create a server from capabilities
 */
fun Iterable<ServerCapability>.asMcp(name: String = "http4k-mcp") =
    mcp(ServerMetaData(name, "0.0.0"), NoMcpSecurity, *toList().toTypedArray())

fun McpResponse.asHttp(): Response = when (this) {
    is Ok -> Response(message.httpStatus())
        .with(Header.CONTENT_TYPE of APPLICATION_JSON)
        .body(McpJson.compact(McpJson.asJsonObject(message)))

    is Accepted -> Response(ACCEPTED)
}

private fun McpJsonRpcMessage.httpStatus(): Status = when (this) {
    is McpJsonRpcErrorResponse -> when (errorCode()) {
        MethodNotFound.code -> NOT_FOUND

        InvalidParams.code, HeaderMismatchError.CODE,
        MissingRequiredClientCapabilityError.CODE, UnsupportedProtocolVersionError.CODE -> BAD_REQUEST

        else -> OK
    }

    else -> OK
}

private fun McpJsonRpcErrorResponse.errorCode(): Int? =
    (error as? MoshiObject)?.get("code")?.let { McpJson.integer(it).toInt() }
