package org.http4k.connect.mcp.messages

import org.http4k.connect.mcp.ClientCapabilites
import org.http4k.connect.mcp.Implementation
import org.http4k.connect.mcp.McpRpcMethod.Companion.NotificationsInitialized
import org.http4k.connect.mcp.ProtocolVersion
import org.http4k.connect.mcp.ProtocolVersion.`2024-11-05`
import org.http4k.connect.mcp.ServerCapabilities

object Initialize {
    data class Request(
        val clientInfo: Implementation,
        val capabilities: ClientCapabilites = ClientCapabilites(),
        val protocolVersion: ProtocolVersion = `2024-11-05`
    )

    data class Response(
        val capabilities: ServerCapabilities = ServerCapabilities(),
        val serverInfo: Implementation,
        val protocolVersion: ProtocolVersion = `2024-11-05`,
        val _meta: Unit? = null,
    )

    data object Notification {
        val method = NotificationsInitialized
    }
}
