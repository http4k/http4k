/*
 * Copyright (c) 2025-present http4k Ltd. All rights reserved.
 * Licensed under the http4k Commercial License: https://http4k.org/commercial-license
 */
package org.http4k.ai.mcp.client.websocket

import org.http4k.ai.mcp.client.McpStreamingClientContract
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

class WebsocketMcpClientTest : McpStreamingClientContract<Websocket>() {
    
    override val doesNotifications = true

    override fun clientSessions() = WebsocketSessions(
        sessionProvider,
        sessionEventTracking,
        sessionEventStore,
    ).apply { start() }

    override fun clientFor(port: Int) = WebsocketMcpClient(
        Request(GET, Uri.of("ws://localhost:${port}/ws")),
        clientName, Version.of("1.0.0"),
        WebsocketClient(Duration.ofSeconds(2), true),
        ClientCapabilities(),
    )

    override fun toPolyHandler(protocol: McpProtocol<Websocket>) = WebsocketMcp(protocol, NoMcpSecurity)
}
