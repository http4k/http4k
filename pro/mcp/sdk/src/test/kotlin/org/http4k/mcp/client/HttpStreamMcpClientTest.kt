package org.http4k.mcp.client

import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.isA
import com.natpryce.hamkrest.present
import dev.forkhandles.result4k.valueOrNull
import org.http4k.client.JavaHttpClient
import org.http4k.connect.model.ToolName
import org.http4k.core.BodyMode.Stream
import org.http4k.core.Response
import org.http4k.core.Uri
import org.http4k.lens.with
import org.http4k.mcp.ToolRequest
import org.http4k.mcp.ToolResponse
import org.http4k.mcp.model.McpEntity
import org.http4k.mcp.model.Tool
import org.http4k.mcp.protocol.ClientCapabilities
import org.http4k.mcp.protocol.ServerMetaData
import org.http4k.mcp.protocol.Version
import org.http4k.mcp.server.http.HttpClientSessions
import org.http4k.mcp.server.http.StandardHttpMcp
import org.http4k.mcp.server.protocol.McpProtocol
import org.http4k.routing.bind
import org.http4k.server.Helidon
import org.http4k.server.asServer
import org.http4k.sse.Sse
import org.junit.jupiter.api.Test

class HttpStreamMcpClientTest : McpClientContract<Sse, Response> {

    override val notifications = false

    override fun clientFor(port: Int) = HttpStreamMcpClient(
        clientName, Version.of("1.0.0"),
        Uri.of("http://localhost:${port}/mcp"),
        JavaHttpClient(responseBodyMode = Stream),
        ClientCapabilities()
    )

    override fun clientSessions() = HttpClientSessions().apply { start() }

    override fun toPolyHandler(protocol: McpProtocol<Sse, Response>) =
        StandardHttpMcp(protocol)

    @Test
    fun `deals with error`() {
        val toolArg = Tool.Arg.required("name")

        val protocol = McpProtocol(
            ServerMetaData(McpEntity.of("David"), Version.of("0.0.1")),
            clientSessions(),
            Tool("reverse", "description", toolArg) bind { error("bad things") }
        )

        val server = toPolyHandler(protocol)
            .asServer(Helidon(0)).start()

        val mcpClient = clientFor(server.port())

        mcpClient.start()

        val actual = mcpClient.tools().call(ToolName.of("reverse"), ToolRequest().with(toolArg of "boom"))
            .valueOrNull()

        assertThat(actual, present(isA<ToolResponse.Error>()))

        mcpClient.stop()
        server.stop()
    }
}
