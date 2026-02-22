package org.http4k.ai.mcp.server

import org.http4k.ai.mcp.protocol.ServerMetaData
import org.http4k.ai.mcp.server.capability.NamedServerCapability
import org.http4k.ai.mcp.server.security.NoMcpSecurity
import org.http4k.routing.mcpHttpStreaming
import org.http4k.server.PolyServerConfig
import org.http4k.server.asServer

/**
 * Convenience function to create a server from a single capability
 */
fun NamedServerCapability.asServer(cfg: PolyServerConfig) = mcpHttpStreaming(
    ServerMetaData(name, "1.0.0"), NoMcpSecurity, this
).asServer(cfg)
