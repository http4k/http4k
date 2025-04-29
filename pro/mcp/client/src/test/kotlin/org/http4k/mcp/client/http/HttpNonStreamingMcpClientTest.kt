package org.http4k.mcp.client.http

import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.equalTo
import org.http4k.client.JavaHttpClient
import org.http4k.core.Method.POST
import org.http4k.core.Request
import org.http4k.core.Uri
import org.http4k.format.renderResult
import org.http4k.mcp.client.McpClientContract
import org.http4k.mcp.model.McpEntity
import org.http4k.mcp.protocol.ServerMetaData
import org.http4k.mcp.protocol.Version
import org.http4k.mcp.protocol.messages.McpPrompt
import org.http4k.mcp.protocol.messages.McpResource
import org.http4k.mcp.protocol.messages.McpTool
import org.http4k.mcp.renderRequest
import org.http4k.mcp.server.http.HttpNonStreamingMcp
import org.http4k.mcp.server.http.HttpStreamingSessions
import org.http4k.mcp.server.protocol.McpProtocol
import org.http4k.mcp.server.security.McpSecurity.Companion.None
import org.http4k.mcp.util.McpJson
import org.http4k.routing.poly
import org.http4k.sse.Sse
import org.junit.jupiter.api.Test

class HttpNonStreamingMcpClientTest : McpClientContract<Sse> {

    override val doesNotifications = false

    override fun clientSessions() = HttpStreamingSessions()

    override fun clientFor(port: Int) = HttpNonStreamingMcpClient(
        Uri.of("http://localhost:${port}/mcp"),
        JavaHttpClient()
    )

    override fun toPolyHandler(protocol: McpProtocol<Sse>) =
        poly(HttpNonStreamingMcp(protocol, None))

    @Test
    fun `can handle batched messages`() {
        val response = toPolyHandler(
            McpProtocol(
                ServerMetaData(McpEntity.of("David"), Version.of("0.0.1")), HttpStreamingSessions(),
            )
        ).http!!(
            Request(POST, "/mcp")
                .body(
                    with(McpJson) {
                        compact(
                            array(
                                listOf(
                                    renderRequest(McpPrompt.List, McpPrompt.List.Request(), 1),
                                    renderRequest(McpResource.List, McpResource.List.Request(), 2),
                                    renderRequest(McpTool.List, McpTool.List.Request(), 3),
                                )
                            )
                        )
                    }
                )
        )

        with(McpJson) {
            assertThat(
                parse(response.bodyString()),
                equalTo(
                    array(
                        listOf(
                            renderResult(asJsonObject(McpPrompt.List.Response(listOf())), number(1)),
                            renderResult(asJsonObject(McpResource.List.Response(listOf())), number(2)),
                            renderResult(asJsonObject(McpTool.List.Response(listOf())), number(3))
                        )
                    )
                )
            )
        }
    }
}
