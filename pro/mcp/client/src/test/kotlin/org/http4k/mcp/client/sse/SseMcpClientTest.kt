package org.http4k.mcp.client.sse

import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.present
import dev.forkhandles.result4k.failureOrNull
import org.http4k.ai.model.ToolName
import org.http4k.client.JavaHttpClient
import org.http4k.core.BodyMode.Stream
import org.http4k.core.Filter
import org.http4k.core.Method.GET
import org.http4k.core.Request
import org.http4k.core.Response
import org.http4k.core.Status.Companion.INTERNAL_SERVER_ERROR
import org.http4k.core.Uri
import org.http4k.core.then
import org.http4k.filter.ClientFilters
import org.http4k.lens.Header
import org.http4k.lens.with
import org.http4k.mcp.ToolRequest
import org.http4k.mcp.ToolResponse
import org.http4k.mcp.client.McpClientContract
import org.http4k.mcp.model.Content
import org.http4k.mcp.model.McpEntity
import org.http4k.mcp.model.Tool
import org.http4k.mcp.model.string
import org.http4k.mcp.protocol.ClientCapabilities
import org.http4k.mcp.protocol.ServerMetaData
import org.http4k.mcp.protocol.Version
import org.http4k.mcp.server.protocol.McpProtocol
import org.http4k.mcp.server.security.ApiKeyMcpSecurity
import org.http4k.mcp.server.sse.SseMcp
import org.http4k.mcp.server.sse.SseSessions
import org.http4k.routing.bind
import org.http4k.server.JettyLoom
import org.http4k.server.asServer
import org.http4k.sse.Sse
import org.junit.jupiter.api.Test

class SseMcpClientTest : McpClientContract<Sse> {

    override val doesNotifications = true

    override fun clientFor(port: Int) = SseMcpClient(
        clientName, Version.of("1.0.0"),
        Request(GET, Uri.of("http://localhost:${port}/sse")),
        ClientFilters.ApiKeyAuth(Header.required("KEY") of "123").then(JavaHttpClient(responseBodyMode = Stream)),
        ClientCapabilities()
    )

    override fun clientSessions() = SseSessions().apply { start() }

    override fun toPolyHandler(protocol: McpProtocol<Sse>) =
        SseMcp(protocol, ApiKeyMcpSecurity(Header.required("KEY"), { it == "123" }))

    @Test
    fun `deals with error`() {
        val toolArg = Tool.Arg.string().required("name")

        val protocol = McpProtocol(
            ServerMetaData(McpEntity.of("David"), Version.of("0.0.1")),
            clientSessions(),
            Tool("reverse", "description", toolArg) bind {
                ToolResponse.Ok(listOf(Content.Text(toolArg(it).reversed())))
            }
        )

        val server = blowUpWhenBoom().then(toPolyHandler(protocol))
            .asServer(JettyLoom(0)).start()

        val mcpClient = clientFor(server.port())

        mcpClient.start()

        assertThat(
            mcpClient.tools().call(ToolName.of("reverse"), ToolRequest().with(toolArg of "boom")).failureOrNull(),
            present()
        )

        mcpClient.stop()
        server.stop()
    }

    private fun blowUpWhenBoom() = Filter { next ->
        {
            if (it.bodyString().contains("boom")) Response(INTERNAL_SERVER_ERROR)
            else next(it)
        }
    }

}
