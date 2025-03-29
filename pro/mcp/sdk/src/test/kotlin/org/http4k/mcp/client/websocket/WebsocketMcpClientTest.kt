package org.http4k.mcp.client.websocket

import org.http4k.client.WebsocketClient
import org.http4k.core.Method.GET
import org.http4k.core.Request
import org.http4k.core.Uri
import org.http4k.mcp.client.McpClientContract
import org.http4k.mcp.protocol.ClientCapabilities
import org.http4k.mcp.protocol.ServerMetaData
import org.http4k.mcp.protocol.Version
import org.http4k.mcp.server.protocol.Completions
import org.http4k.mcp.server.protocol.McpProtocol
import org.http4k.mcp.server.protocol.Prompts
import org.http4k.mcp.server.protocol.Resources
import org.http4k.mcp.server.protocol.Tools
import org.http4k.mcp.server.websocket.WebsocketMcp
import org.http4k.mcp.server.websocket.WebsocketSessions
import org.http4k.websocket.Websocket
import java.time.Duration

class WebsocketMcpClientTest : McpClientContract<Websocket> {

    override val doesNotifications = true

    fun protocol(
        serverMetaData: ServerMetaData,
        prompts: Prompts,
        tools: Tools,
        resources: Resources,
        completions: Completions
    ) = McpProtocol(serverMetaData, clientSessions(), tools, resources, prompts, completions)

    override fun clientSessions() = WebsocketSessions().apply { start() }

    override fun clientFor(port: Int) = WebsocketMcpClient(
        clientName, Version.of("1.0.0"),
        Request(GET, Uri.of("ws://localhost:${port}/ws")),
        WebsocketClient(Duration.ofSeconds(2), true),
        ClientCapabilities(),
    )

    override fun toPolyHandler(protocol: McpProtocol<Websocket>) = WebsocketMcp(protocol)
}
