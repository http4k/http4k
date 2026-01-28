package org.http4k.ai.mcp.client.websocket

import org.http4k.ai.mcp.client.McpClientContract
import org.http4k.ai.mcp.protocol.ClientCapabilities
import org.http4k.ai.mcp.protocol.Version
import org.http4k.ai.mcp.server.protocol.McpProtocol
import org.http4k.ai.mcp.server.security.NoMcpSecurity
import org.http4k.ai.mcp.server.websocket.WebsocketMcp
import org.http4k.ai.mcp.server.websocket.WebsocketSessions
import org.http4k.client.WebsocketClient
import org.http4k.core.Method.GET
import org.http4k.core.Request
import org.http4k.core.Uri
import org.http4k.websocket.Websocket
import java.time.Duration

class WebsocketMcpClientTest : McpClientContract<Websocket> {

    override val doesNotifications = true

    override fun clientSessions() = WebsocketSessions().apply { start() }

    override fun clientFor(port: Int) = WebsocketMcpClient(
        clientName, Version.of("1.0.0"),
        Request(GET, Uri.of("ws://localhost:${port}/ws")),
        WebsocketClient(Duration.ofSeconds(2), true),
        ClientCapabilities(),
    )

    override fun toPolyHandler(protocol: McpProtocol<Websocket>) = WebsocketMcp(protocol, NoMcpSecurity)
}
