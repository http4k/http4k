package org.http4k.ai.mcp.client

import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.equalTo
import dev.forkhandles.result4k.valueOrNull
import org.http4k.ai.mcp.ToolResponse.Ok
import org.http4k.ai.mcp.client.http.HttpStreamingMcpClient
import org.http4k.ai.mcp.model.Content
import org.http4k.ai.mcp.model.McpEntity
import org.http4k.ai.mcp.model.Tool
import org.http4k.ai.mcp.protocol.ClientCapabilities
import org.http4k.ai.mcp.protocol.ServerMetaData
import org.http4k.ai.mcp.protocol.Version
import org.http4k.ai.mcp.server.security.OAuthMcpSecurity
import org.http4k.ai.model.ToolName
import org.http4k.client.JavaHttpClient
import org.http4k.client.ReconnectionMode.Disconnect
import org.http4k.core.BodyMode.Stream
import org.http4k.core.ContentType.Companion.APPLICATION_JSON
import org.http4k.core.Credentials
import org.http4k.core.Response
import org.http4k.core.Status.Companion.OK
import org.http4k.core.Uri
import org.http4k.core.then
import org.http4k.filter.ClientFilters
import org.http4k.lens.contentType
import org.http4k.routing.bind
import org.http4k.routing.mcpHttpStreaming
import org.http4k.routing.routes
import org.http4k.security.OAuthWebForms.requestForm
import org.http4k.security.OAuthWebForms.resource
import org.http4k.security.ResponseType.Code
import org.http4k.security.oauth.metadata.AuthMethod.client_secret_basic
import org.http4k.security.oauth.metadata.ServerMetadata
import org.http4k.security.oauth.server.AuthorizationServerWellKnown
import org.http4k.server.Helidon
import org.http4k.server.asServer
import org.http4k.util.PortBasedTest
import org.junit.jupiter.api.Test

class DiscoveredMcpOAuthTest : PortBasedTest {

    private val protectedResourceUri = Uri.of("http://localhost:32323/mcp")
    private val authServer = routes(
        "/token" bind {
            assertThat(resource(requestForm(it)), equalTo(protectedResourceUri))
            Response(OK)
                .contentType(APPLICATION_JSON)
                .body(
                    """
                    {
                        "access_token": "test-token",
                        "token_type": "bearer",
                        "expires_in": 3600
                    }
                    """.trimIndent()
                )
        },
        AuthorizationServerWellKnown(
            ServerMetadata(
                "foobar",
                Uri.of("/authorization"),
                Uri.of("/token"),
                listOf(client_secret_basic),
                listOf("RS256"),
                listOf(Code),
                listOf("read", "write")
            )
        ),
    ).asServer(Helidon(0))

    @Test
    fun `can discover auth token from protected resource`() {
        authServer.start()

        var count = 0

        val mcpServer = mcpHttpStreaming(
            ServerMetaData(McpEntity.of("http4k mcp server"), Version.of("0.1.0")),
            OAuthMcpSecurity(
                Uri.of("http://localhost:${authServer.port()}"),
                protectedResourceUri
            ) { it == "test-token" },
            Tool("hello", "say hello") bind {
                Ok(listOf(Content.Text("helloworld${count++}")))
            }
        ).asServer(Helidon(32323)).start()

        val http = JavaHttpClient(responseBodyMode = Stream)

        HttpStreamingMcpClient(
            McpEntity.of("client"), Version.of("1.0.0"),
            Uri.of("http://localhost:${mcpServer.port()}/mcp"),
            ClientFilters.DiscoveredMcpOAuth(Credentials("123", "123"), listOf("read", "write")).then(http),
            ClientCapabilities(),
            notificationSseReconnectionMode = Disconnect,
        ).use {
            assertThat(it.tools().call(ToolName.of("hello")).valueOrNull(), equalTo(Ok("helloworld0")))
            assertThat(it.tools().call(ToolName.of("hello")).valueOrNull(), equalTo(Ok("helloworld1")))
        }
    }
}
