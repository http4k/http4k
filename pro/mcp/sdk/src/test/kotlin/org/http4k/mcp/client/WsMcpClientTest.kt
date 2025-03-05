package org.http4k.mcp.client

import org.http4k.client.WebsocketClient
import org.http4k.core.Method.GET
import org.http4k.core.Request
import org.http4k.core.Response
import org.http4k.core.Uri
import org.http4k.mcp.model.McpEntity
import org.http4k.mcp.protocol.ClientCapabilities
import org.http4k.mcp.protocol.ServerMetaData
import org.http4k.mcp.protocol.Version
import org.http4k.mcp.server.capability.Completions
import org.http4k.mcp.server.capability.Prompts
import org.http4k.mcp.server.capability.Resources
import org.http4k.mcp.server.capability.Tools
import org.http4k.mcp.server.protocol.McpProtocol
import org.http4k.mcp.server.ws.StandardWsMcp
import org.http4k.mcp.server.ws.WsMcpSession
import org.http4k.websocket.Websocket
import java.time.Duration

class WsMcpClientTest : McpClientContract<Websocket, Response, WsMcpSession> {

    override val notifications = true

    override fun transport(
        serverMetaData: ServerMetaData,
        prompts: Prompts,
        tools: Tools,
        resources: Resources,
        completions: Completions,
    ) = WsMcpSession(McpProtocol(serverMetaData, tools, resources, prompts, completions))

    override fun clientFor(port: Int) = WsMcpClient(
        McpEntity.of("foobar"), Version.of("1.0.0"),
        ClientCapabilities(),
        Request(GET, Uri.of("ws://localhost:${port}/ws")),
        WebsocketClient(Duration.ofSeconds(2), true),
    )

    override fun toPolyHandler(transport: WsMcpSession) = StandardWsMcp(transport)
}
