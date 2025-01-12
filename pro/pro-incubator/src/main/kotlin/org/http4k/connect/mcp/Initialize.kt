package org.http4k.connect.mcp

import org.http4k.connect.mcp.HasMeta.Companion.default
import org.http4k.connect.mcp.McpRpcMethod.Companion.of
import org.http4k.connect.mcp.ProtocolVersion.`2024-11-05`

object Initialize : HasMethod {
    override val Method = McpRpcMethod.of("initialize")

    data class Request(
        val clientInfo: Implementation,
        val capabilities: ClientCapabilites = ClientCapabilites(),
        val protocolVersion: ProtocolVersion = `2024-11-05`
    ) : ClientRequest

    data class Response(
        val capabilities: ServerCapabilities = ServerCapabilities(),
        val serverInfo: Implementation,
        val protocolVersion: ProtocolVersion = `2024-11-05`,
        override val _meta: Map<String, Any> = default,
    ) : HasMeta, ServerResponse

    data object Notification : ServerNotification {
        override val method = of("notifications/initialized")
    }
}
