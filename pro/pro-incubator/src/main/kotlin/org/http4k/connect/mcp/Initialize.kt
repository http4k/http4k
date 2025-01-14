package org.http4k.connect.mcp

import org.http4k.connect.mcp.HasMeta.Companion.default
import org.http4k.connect.mcp.McpRpcMethod.Companion.of
import org.http4k.connect.mcp.ProtocolVersion.Companion.LATEST_VERSION

object Initialize : HasMethod {
    override val Method = McpRpcMethod.of("initialize")

    data class Request(
        val clientInfo: Implementation,
        val capabilities: ClientCapabilites = ClientCapabilites(),
        val protocolVersion: ProtocolVersion = LATEST_VERSION
    ) : ClientRequest

    data class Response(
        val capabilities: ServerCapabilities = ServerCapabilities(),
        val serverInfo: Implementation,
        val protocolVersion: ProtocolVersion = LATEST_VERSION,
        override val _meta: Map<String, Any> = default,
    ) : HasMeta, ServerResponse

    object Notification : ClientRequest, HasMethod {
        override val Method = of("notifications/initialized")
    }
}
