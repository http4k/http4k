package org.http4k.mcp.client.http

import org.http4k.client.JavaHttpClient
import org.http4k.core.Response
import org.http4k.core.Uri
import org.http4k.mcp.client.McpClientContract
import org.http4k.mcp.server.http.HttpNonStreamingMcpConnection
import org.http4k.mcp.server.http.HttpStreamingSessions
import org.http4k.mcp.server.protocol.McpProtocol
import org.http4k.routing.poly
import org.http4k.sse.Sse

class HttpNonStreamingMcpClientTest : McpClientContract<Sse, Response> {

    override val notifications = false

    override fun clientSessions() = HttpStreamingSessions()

    override fun clientFor(port: Int) = HttpNonStreamingMcpClient(
        Uri.of("http://localhost:${port}/mcp"),
        JavaHttpClient()
    )

    override fun toPolyHandler(protocol: McpProtocol<Sse, Response>) = poly(
        HttpNonStreamingMcpConnection(protocol)
    )
}
