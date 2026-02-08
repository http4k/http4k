package org.http4k.ai.mcp.apps

import org.http4k.ai.mcp.client.McpClient
import org.http4k.ai.mcp.client.http.HttpStreamingMcpClient
import org.http4k.ai.mcp.model.McpEntity
import org.http4k.ai.mcp.protocol.Version
import org.http4k.ai.mcp.testing.TestMcpClient
import org.http4k.client.JavaHttpClient
import org.http4k.core.BodyMode.Stream
import org.http4k.core.HttpHandler
import org.http4k.core.Method.GET
import org.http4k.core.PolyHandler
import org.http4k.core.Request
import org.http4k.core.Uri
import org.http4k.server.JettyLoom
import org.http4k.server.asServer

fun interface McpClientFactory {
    operator fun invoke(server: PolyHandler): McpClient

    companion object {
        fun Http(http: HttpHandler = JavaHttpClient(responseBodyMode = Stream)) = McpClientFactory {
            val server = it.asServer(JettyLoom(0)).start()

            HttpStreamingMcpClient(
                McpEntity.of("http4k MCP Testing"),
                Version.of("0.0.0"),
                Uri.of("http://localhost:${server.port()}/mcp"), http
            )
        }

        fun Test() = McpClientFactory { TestMcpClient(it, Request(GET, "/mcp")) }
    }
}
