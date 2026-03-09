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
fun ServerCapability.asServer(cfg: PolyServerConfig) = mcp(
    ServerMetaData(name, "1.0.0"), NoMcpSecurity, this
).asServer(cfg)
