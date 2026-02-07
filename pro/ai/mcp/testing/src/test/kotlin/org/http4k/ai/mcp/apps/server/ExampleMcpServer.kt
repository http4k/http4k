package org.http4k.ai.mcp.apps.server

import org.http4k.ai.mcp.model.extension.McpApps
import org.http4k.ai.mcp.protocol.ServerMetaData
import org.http4k.ai.mcp.protocol.withExtensions
import org.http4k.ai.mcp.server.security.NoMcpSecurity
import org.http4k.routing.mcpHttpStreaming
import org.http4k.server.Helidon
import org.http4k.server.asServer
import org.http4k.template.HandlebarsTemplates

fun ExampleMcpServer(port: Int) = mcpHttpStreaming(
    ServerMetaData("order server", "0.0.1").withExtensions(McpApps),
    NoMcpSecurity,
    OrderFormUi.resource(HandlebarsTemplates().CachingClasspath()),
    ShowOrderFormTool(),
    SubmitOrderTool()
).asServer(Helidon(port))
