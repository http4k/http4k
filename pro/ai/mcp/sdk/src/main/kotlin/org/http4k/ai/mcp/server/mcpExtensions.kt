/*
 * Copyright (c) 2025-present http4k Ltd. All rights reserved.
 * Licensed under the http4k Commercial License: https://http4k.org/commercial-license
 */
package org.http4k.ai.mcp.server

import org.http4k.ai.mcp.protocol.ServerMetaData
import org.http4k.ai.mcp.server.capability.ServerCapability
import org.http4k.ai.mcp.server.protocol.McpResponse
import org.http4k.ai.mcp.server.protocol.McpResponse.Accepted
import org.http4k.ai.mcp.server.protocol.McpResponse.Ok
import org.http4k.ai.mcp.server.protocol.McpResponse.Unknown
import org.http4k.ai.mcp.server.security.NoMcpSecurity
import org.http4k.ai.mcp.util.McpJson
import org.http4k.ai.mcp.util.McpNodeType
import org.http4k.core.ContentType
import org.http4k.core.Response
import org.http4k.core.Status
import org.http4k.core.Status.Companion.ACCEPTED
import org.http4k.core.Status.Companion.NOT_FOUND
import org.http4k.core.with
import org.http4k.format.MoshiNull
import org.http4k.lens.Header
import org.http4k.routing.mcp
import org.http4k.server.PolyServerConfig
import org.http4k.server.asServer
import org.http4k.sse.SseMessage.Event

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


fun McpResponse.asHttp(status: Status) =
    when (val response = this) {
        is Ok -> McpJson.asJsonObject(response.message).asHttp(status)
        is Accepted -> McpJson.nullNode().asHttp(ACCEPTED)
        is Unknown -> McpJson.nullNode().asHttp(NOT_FOUND)
    }

private fun McpNodeType.asHttp(status: Status) = when (this) {
    is MoshiNull -> Response(status)
    else -> Response(status)
        .with(Header.CONTENT_TYPE of ContentType.TEXT_EVENT_STREAM)
        .body(Event("message", McpJson.asFormatString(this)).toMessage())
}
