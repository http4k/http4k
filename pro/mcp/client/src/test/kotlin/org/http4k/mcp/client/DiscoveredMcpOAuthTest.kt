package org.http4k.mcp.client

import org.http4k.client.JavaHttpClient
import org.http4k.client.ReconnectionMode.Disconnect
import org.http4k.connect.model.ToolName
import org.http4k.core.BodyMode.Stream
import org.http4k.core.Credentials
import org.http4k.core.Response
import org.http4k.core.Status.Companion.OK
import org.http4k.core.Uri
import org.http4k.core.then
import org.http4k.filter.ClientFilters
import org.http4k.filter.debug
import org.http4k.mcp.ToolResponse
import org.http4k.mcp.client.http.HttpStreamingMcpClient
import org.http4k.mcp.model.Content
import org.http4k.mcp.model.McpEntity
import org.http4k.mcp.model.Tool
import org.http4k.mcp.protocol.ClientCapabilities
import org.http4k.mcp.protocol.ServerMetaData
import org.http4k.mcp.protocol.Version
import org.http4k.mcp.server.security.OAuthMcpSecurity
import org.http4k.routing.bind
import org.http4k.routing.mcpHttpStreaming
import org.http4k.routing.routes
import org.http4k.server.Helidon
import org.http4k.server.asServer
import org.junit.jupiter.api.Test

class DiscoveredMcpOAuthTest {


    @Test
    fun `asdasd`() {

        val authServer = routes(
            "/custom/token" bind {
                Response(OK).body(
                    """
                    {
                        "access_token": "test-token",
                        "token_type": "bearer",
                        "expires_in": 3600
                    }
                    """.trimIndent()
                )
            }
        ).asServer(Helidon(0)).start()

        val mcpServer = mcpHttpStreaming(
            ServerMetaData(McpEntity.of("http4k mcp server"), Version.of("0.1.0")),
            OAuthMcpSecurity(Uri.of("http://localhost:${authServer.port()}")) { it == "test-token" },
            Tool("hello", "say hello") bind {
                ToolResponse.Ok(listOf(Content.Text("helloworld")))
            }
        ).debug().asServer(Helidon(0)).start()

        val http = JavaHttpClient(responseBodyMode = Stream).debug(debugStream = true)

        HttpStreamingMcpClient(
            McpEntity.of("client"), Version.of("1.0.0"),
            Uri.of("http://localhost:${mcpServer.port()}/mcp"),
            ClientFilters.DiscoveredMcpOAuth(Credentials("123", "123")).then(http),
            ClientCapabilities(),
            notificationSseReconnectionMode = Disconnect,
        ).use {
            println(it.tools().call(ToolName.of("hello")))
        }

    }
}
