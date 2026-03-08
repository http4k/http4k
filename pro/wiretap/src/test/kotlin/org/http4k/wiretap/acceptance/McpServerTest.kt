package org.http4k.wiretap.acceptance

import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.equalTo
import com.natpryce.hamkrest.greaterThan
import org.http4k.ai.mcp.ToolResponse.Ok
import org.http4k.ai.mcp.client.http.HttpNonStreamingMcpClient
import org.http4k.ai.mcp.model.Content.Text
import org.http4k.ai.mcp.model.McpEntity
import org.http4k.ai.mcp.model.Tool
import org.http4k.ai.mcp.protocol.ServerMetaData
import org.http4k.ai.mcp.protocol.Version
import org.http4k.ai.mcp.server.security.NoMcpSecurity
import org.http4k.ai.mcp.testing.testMcpClient
import org.http4k.ai.model.ToolName
import org.http4k.client.Http4kSseClient
import org.http4k.core.Method.GET
import org.http4k.core.Method.POST
import org.http4k.core.Request
import org.http4k.core.Uri
import org.http4k.routing.bind
import org.http4k.routing.mcpHttpStreaming
import org.http4k.server.Helidon
import org.http4k.server.asServer
import org.http4k.server.uri
import org.http4k.testing.testSseClient
import org.http4k.util.FixedClock
import org.http4k.wiretap.Wiretap
import org.http4k.wiretap.WiretappedUriProvider
import org.http4k.wiretap.util.Json
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class McpServerTest : WiretapSmokeContract {

    override val testRequest = Request(GET, Uri.of("/mcp"))

    private val server = mcpHttpStreaming(
        ServerMetaData(McpEntity.of("123"), Version.of("123")),
        NoMcpSecurity,
        Tool("foo", "bar") bind { Ok("Hello") }
    )
        .asServer(Helidon(0))

    override lateinit var uriProvider: WiretappedUriProvider

    @Test
    fun `mcp transactions through wiretap are stored`() {
        val wiretap = Wiretap(
            clock = FixedClock,
            uriProvider = uriProvider
        )

        HttpNonStreamingMcpClient(Uri.of("/mcp"), http = wiretap.http!!).apply { start() }.use {
            val list = it.tools().list().orThrowIt()
            assertThat(list.size, equalTo(1))
        }

        wiretap.testMcpClient(Request(POST, "_wiretap/mcp")).use {
            val call = it.tools().call(ToolName.of("list_transactions")).orThrowIt()

            val calls = (call as Ok).content!![0] as Text
            val elements = Json.elements(Json.parse(calls.text))
            assertThat(elements.size, equalTo(4))
        }

    }

    @BeforeEach
    fun start() {
        server.start()
        uriProvider = WiretappedUriProvider { _, _ -> server.uri() }
    }

    @BeforeEach
    fun stop() {
        server.stop()
    }
}
