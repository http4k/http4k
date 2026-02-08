package org.http4k.ai.mcp.apps

import org.http4k.ai.mcp.apps.server.ExampleMcpServer
import org.http4k.ai.mcp.testing.McpClientFactory
import org.http4k.core.Uri
import org.http4k.server.JettyLoom
import org.http4k.server.SunHttp
import org.http4k.server.asServer

fun main() {
    val server = ExampleMcpServer().asServer(JettyLoom(3001)).start()

    val host = McpAppsHost(McpClientFactory.Http(Uri.of("http://localhost:${server.port()}/mcp")))
        .asServer(SunHttp(9000)).start()

    println("MCP Apps Host running on http://localhost:${host.port()}")
}
