package org.http4k.ai.mcp.testing

import org.http4k.ai.mcp.client.McpClient
import org.http4k.ai.mcp.client.http.HttpStreamingMcpClient
import org.http4k.ai.mcp.model.McpEntity
import org.http4k.ai.mcp.protocol.Version
import org.http4k.client.JavaHttpClient
import org.http4k.core.BodyMode
import org.http4k.core.HttpHandler
import org.http4k.core.Method.POST
import org.http4k.core.PolyHandler
import org.http4k.core.Request
import org.http4k.core.Uri
import org.http4k.server.JettyLoom
import org.http4k.server.asServer

/**
 * Easy MCP Client creation for testing purposes.
 */
fun interface McpClientFactory : () -> McpClient {

    companion object {
        /**
         * Starts a local MCP server and returns an McpClient connected to it.
         */
        fun Local(mcpServer: PolyHandler, http: HttpHandler = JavaHttpClient(responseBodyMode = BodyMode.Stream)) =
            McpClientFactory {
                val server = mcpServer.asServer(JettyLoom(0)).start()
                Http(Uri.Companion.of("http://localhost:${server.port()}/mcp"), http)()
            }

        /**
         * Returns an McpClient connected to the given remote server.
         */
        fun Http(serverUri: Uri, http: HttpHandler = JavaHttpClient(responseBodyMode = BodyMode.Stream)) =
            McpClientFactory {
                HttpStreamingMcpClient(
                    McpEntity.of("http4k MCP Testing"),
                    Version.of("0.0.0"),
                    serverUri, http
                )
            }

        /**
         * Creates an in-memory MCP server and returns an McpClient connected to it.
         */
        fun Test(mcpServer: PolyHandler) = McpClientFactory { mcpServer.testMcpClient(Request(POST, "/mcp")) }
    }
}
