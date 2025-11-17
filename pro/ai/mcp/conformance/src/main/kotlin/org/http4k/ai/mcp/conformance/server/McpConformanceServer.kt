package org.http4k.ai.mcp.conformance.server

import org.http4k.ai.mcp.conformance.server.misc.ConformanceMisc
import org.http4k.ai.mcp.conformance.server.prompts.CondormancePrompts
import org.http4k.ai.mcp.conformance.server.resources.ConformanceResources
import org.http4k.ai.mcp.conformance.server.tools.ConformanceTools
import org.http4k.ai.mcp.model.McpEntity
import org.http4k.ai.mcp.protocol.ServerMetaData
import org.http4k.ai.mcp.protocol.ServerProtocolCapability
import org.http4k.ai.mcp.protocol.Version
import org.http4k.ai.mcp.server.security.NoMcpSecurity
import org.http4k.filter.debugMcp
import org.http4k.routing.mcpHttpStreaming
import org.http4k.server.JettyLoom
import org.http4k.server.asServer

/**
 * Server which implements the MCP Conformance test suite using the http4k MCP SDK
 */
fun McpConformanceServer() = mcpHttpStreaming(
    ServerMetaData(
        McpEntity.of("http4k MCP conformance"), Version.of("0.1.0"),
        *ServerProtocolCapability.entries.toTypedArray()
    ),
    NoMcpSecurity,
    ConformanceTools(),
    ConformanceResources(),
    CondormancePrompts(),
    ConformanceMisc()
)

fun main() {
    McpConformanceServer().debugMcp().asServer(JettyLoom(4001)).start()
}

