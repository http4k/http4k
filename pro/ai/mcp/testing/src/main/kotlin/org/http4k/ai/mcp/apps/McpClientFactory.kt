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

fun interface McpClientFactory {
    operator fun invoke(server: Uri): McpClient

    companion object {
        fun Http(http: HttpHandler = JavaHttpClient(responseBodyMode = Stream)) = McpClientFactory {
            HttpStreamingMcpClient(
                McpEntity.of("http4k MCP Testing"),
                Version.of("0.0.0"),
                it, http
            )
        }

        fun Test(mcp: PolyHandler) = McpClientFactory { TestMcpClient(mcp, Request(GET, "/mcp")) }
    }
}
