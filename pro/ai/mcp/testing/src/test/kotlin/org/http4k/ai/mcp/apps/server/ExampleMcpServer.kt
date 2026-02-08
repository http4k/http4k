package org.http4k.ai.mcp.apps.server

import org.http4k.ai.mcp.model.apps.McpApps
import org.http4k.ai.mcp.protocol.ServerMetaData
import org.http4k.ai.mcp.protocol.withExtensions
import org.http4k.ai.mcp.server.security.NoMcpSecurity
import org.http4k.filter.debugMcp
import org.http4k.routing.mcpHttpStreaming

fun ExampleMcpServer() = mcpHttpStreaming(
    ServerMetaData("order server", "0.0.0").withExtensions(McpApps),
    NoMcpSecurity,
    OrderFormMcpApp(),
).debugMcp()
