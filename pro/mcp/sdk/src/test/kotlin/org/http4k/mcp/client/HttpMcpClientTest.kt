package org.http4k.mcp.client

import org.http4k.client.JavaHttpClient
import org.http4k.core.Response
import org.http4k.core.Uri
import org.http4k.mcp.protocol.ServerMetaData
import org.http4k.mcp.server.capability.Completions
import org.http4k.mcp.server.capability.Prompts
import org.http4k.mcp.server.capability.Resources
import org.http4k.mcp.server.capability.Tools
import org.http4k.mcp.server.http.EventStreamMcpTransport
import org.http4k.mcp.server.http.Http
import org.http4k.mcp.server.http.StandardHttpMcp
import org.http4k.mcp.server.protocol.McpProtocol
import org.http4k.mcp.server.session.McpSession
import org.http4k.sse.Sse

class HttpMcpClientTest : McpClientContract<Sse, Response, EventStreamMcpTransport> {

    override val notifications = false

    override fun transport(
        serverMetaData: ServerMetaData,
        prompts: Prompts,
        tools: Tools,
        resources: Resources,
        completions: Completions
    ) = EventStreamMcpTransport(
        McpProtocol(serverMetaData, tools, resources, prompts, completions),
        McpSession.Http()
    ).also { it.start() }

    override fun clientFor(port: Int) = HttpMcpClient(
        Uri.of("http://localhost:${port}/sse"),
        JavaHttpClient()
    )

    override fun toPolyHandler(transport: EventStreamMcpTransport) = StandardHttpMcp(transport)
}
