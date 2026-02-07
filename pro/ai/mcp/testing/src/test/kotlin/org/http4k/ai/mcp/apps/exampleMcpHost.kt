package org.http4k.ai.mcp.apps

import org.http4k.ai.mcp.protocol.ServerMetaData
import org.http4k.ai.mcp.server.security.NoMcpSecurity
import org.http4k.client.JavaHttpClient
import org.http4k.core.Uri
import org.http4k.filter.debug
import org.http4k.routing.mcpHttpStreaming
import org.http4k.server.Helidon
import org.http4k.server.SunHttp
import org.http4k.server.asServer

fun main() {
    val server = mcpHttpStreaming(
        ServerMetaData("server", "0.0.1"),
        NoMcpSecurity,
        OrderFormUi.resource,
        showOrderFormTool(),
        submitOrderTool()
    )
        .asServer(Helidon(0)).start()

    val host = McpAppsHost(
        listOf(Uri.of("http://localhost:${server.port()}/mcp")),
        JavaHttpClient().debug()
    )
        .debug()
        .asServer(SunHttp(9000)).start()

    println("MCP Apps Host running on http://localhost:${host.port()}")
}
