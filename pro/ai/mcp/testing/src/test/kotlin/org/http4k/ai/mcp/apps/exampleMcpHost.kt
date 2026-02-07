package org.http4k.ai.mcp.apps

import org.http4k.client.JavaHttpClient
import org.http4k.core.Uri
import org.http4k.server.SunHttp
import org.http4k.server.asServer

fun main() {
    val server = McpServer(0).start()

    val host = McpAppsHost(
        listOf(Uri.of("http://localhost:${server.port()}/mcp")),
        JavaHttpClient()
    )
        .asServer(SunHttp(9000)).start()

    println("MCP Apps Host running on http://localhost:${host.port()}")
}
