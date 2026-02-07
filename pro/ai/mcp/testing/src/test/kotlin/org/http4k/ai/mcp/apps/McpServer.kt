package org.http4k.ai.mcp.apps

import org.http4k.ai.mcp.protocol.ServerMetaData
import org.http4k.ai.mcp.server.security.NoMcpSecurity
import org.http4k.core.PolyHandler
import org.http4k.routing.mcpHttpStreaming
import org.http4k.server.Helidon
import org.http4k.server.Http4kServer
import org.http4k.server.asServer
import org.http4k.template.HandlebarsTemplates

fun McpServer(port: Int): Http4kServer {
    val templates = HandlebarsTemplates().CachingClasspath()
    return mcpHttpStreaming(
        ServerMetaData("server", "0.0.1"),
        NoMcpSecurity,
        OrderFormUi.resource(templates),
        showOrderFormTool(),
        submitOrderTool()
    ).asServer(Helidon(port))
}
