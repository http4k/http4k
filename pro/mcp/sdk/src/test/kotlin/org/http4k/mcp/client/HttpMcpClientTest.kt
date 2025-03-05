package org.http4k.mcp.client

import org.http4k.client.JavaHttpClient
import org.http4k.core.Response
import org.http4k.core.Uri
import org.http4k.mcp.protocol.ServerMetaData
import org.http4k.mcp.server.RealtimeMcpProtocol
import org.http4k.mcp.server.capability.Completions
import org.http4k.mcp.server.capability.Prompts
import org.http4k.mcp.server.capability.Resources
import org.http4k.mcp.server.capability.Tools
import org.http4k.mcp.server.http.Http
import org.http4k.mcp.server.http.StandardHttpMcp
import org.http4k.mcp.server.session.McpSession
import org.http4k.sse.Sse

class HttpMcpClientTest : McpClientContract<Response, RealtimeMcpProtocol<Sse>> {

    override val notifications = false

    override fun protocol(
        serverMetaData: ServerMetaData,
        prompts: Prompts,
        tools: Tools,
        resources: Resources,
        completions: Completions
    ) = RealtimeMcpProtocol(
        McpSession.Http(),
        serverMetaData, prompts, tools, resources, completions
    )

    override fun clientFor(port: Int) = HttpMcpClient(
        Uri.of("http://localhost:${port}/sse"),
        JavaHttpClient()
    )

    override fun toPolyHandler(protocol: RealtimeMcpProtocol<Sse>) = StandardHttpMcp(protocol)
}
