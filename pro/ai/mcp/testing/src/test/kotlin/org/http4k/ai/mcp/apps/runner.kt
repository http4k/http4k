package org.http4k.ai.mcp.apps

import org.http4k.ai.mcp.apps.server.ExampleMcpServer
import org.http4k.ai.mcp.testing.McpClientFactory
import org.http4k.server.SunHttp
import org.http4k.server.asServer

fun main() {
    val host = McpAppsHost(McpClientFactory.Test(ExampleMcpServer()))
        .asServer(SunHttp(9000)).start()

    println("MCP Apps Host running on http://localhost:${host.port()}")
}
