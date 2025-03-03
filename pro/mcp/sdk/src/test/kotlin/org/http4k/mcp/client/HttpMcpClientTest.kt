package org.http4k.mcp.client

import org.http4k.client.JavaHttpClient
import org.http4k.client.WebsocketClient
import org.http4k.core.Method.GET
import org.http4k.core.Request
import org.http4k.core.Response
import org.http4k.core.Uri
import org.http4k.mcp.model.McpEntity
import org.http4k.mcp.protocol.ClientCapabilities
import org.http4k.mcp.protocol.ServerMetaData
import org.http4k.mcp.protocol.Version
import org.http4k.mcp.server.RealtimeMcpProtocol
import org.http4k.mcp.server.capability.Completions
import org.http4k.mcp.server.capability.Sampling
import org.http4k.mcp.server.capability.Prompts
import org.http4k.mcp.server.capability.Resources
import org.http4k.mcp.server.capability.Tools
import org.http4k.mcp.server.http.Http
import org.http4k.mcp.server.http.StandardHttpMcpHandler
import org.http4k.mcp.server.session.McpSession
import org.http4k.mcp.server.ws.StandardWsMcpHandler
import org.http4k.mcp.server.ws.Websocket
import org.http4k.sse.Sse
import org.http4k.websocket.Websocket
import java.time.Duration

class HttpMcpClientTest : McpClientContract<Response, RealtimeMcpProtocol<Sse>> {

    override val notifications = false

    override fun protocol(
        serverMetaData: ServerMetaData,
        prompts: Prompts,
        tools: Tools,
        resources: Resources,
        completions: Completions,
        incomingSampling: Sampling
    ) = RealtimeMcpProtocol(
        McpSession.Http(),
        serverMetaData, prompts, tools, resources, completions, incomingSampling
    )

    override fun clientFor(port: Int) = HttpMcpClient(
        Uri.of("http://localhost:${port}/sse"),
        JavaHttpClient()
    )

    override fun toPolyHandler(protocol: RealtimeMcpProtocol<Sse>) = StandardHttpMcpHandler(protocol)
}
