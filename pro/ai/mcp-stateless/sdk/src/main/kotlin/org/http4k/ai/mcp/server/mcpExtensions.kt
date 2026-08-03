/*
 * Copyright (c) 2025-present http4k Ltd. All rights reserved.
 * Licensed under the http4k Commercial License: https://http4k.org/commercial-license
 */
package org.http4k.ai.mcp.server

import org.http4k.ai.mcp.protocol.ServerMetaData
import org.http4k.ai.mcp.server.capability.ServerCapability
import org.http4k.ai.mcp.server.security.NoMcpSecurity
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
