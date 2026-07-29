/*
 * Copyright (c) 2025-present http4k Ltd. All rights reserved.
 * Licensed under the http4k Commercial License: https://http4k.org/commercial-license
 */
package org.http4k.ai.mcp.server

import org.http4k.ai.mcp.protocol.ServerMetaData
import org.http4k.ai.mcp.protocol.VersionedMcpEntity
import org.http4k.ai.mcp.server.capability.ServerCapability
import org.http4k.ai.mcp.server.protocol.McpResponse
import org.http4k.ai.mcp.server.protocol.McpResponse.Accepted
import org.http4k.ai.mcp.server.protocol.McpResponse.Ok
import org.http4k.ai.mcp.server.protocol.McpResponse.Unknown
import org.http4k.ai.mcp.server.security.NoMcpSecurity
import org.http4k.ai.mcp.util.McpJson
import org.http4k.core.ContentType.Companion.APPLICATION_JSON
import org.http4k.core.Response
import org.http4k.core.Status.Companion.ACCEPTED
import org.http4k.core.Status.Companion.NOT_FOUND
import org.http4k.core.Status.Companion.OK
import org.http4k.core.with
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

// Stateless single-JSON response; serverInfo is stamped into result _meta here (the one funnel).
fun McpResponse.asHttp(serverInfo: VersionedMcpEntity): Response = when (this) {
    is Ok -> Response(OK)
        .with(Header.CONTENT_TYPE of APPLICATION_JSON)
        .body(McpJson.compact(McpJson.asJsonObject(message).withServerInfo(serverInfo)))

    is Accepted -> Response(ACCEPTED)
    is Unknown -> Response(NOT_FOUND)
}
