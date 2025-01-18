package org.http4k.mcp.protocol

import org.http4k.mcp.protocol.HasMeta.Companion.default
import org.http4k.mcp.protocol.McpRpcMethod.Companion.of
import org.http4k.mcp.protocol.ProtocolVersion.Companion.LATEST_VERSION

object McpInitialize : HasMethod {
    override val Method = McpRpcMethod.of("initialize")

    data class Request(
        val clientInfo: Implementation,
        val capabilities: org.http4k.mcp.protocol.ClientCapabilites = org.http4k.mcp.protocol.ClientCapabilites(),
        val protocolVersion: ProtocolVersion = LATEST_VERSION
    ) : ClientMessage.Request

    data class Response(
        val capabilities: ServerCapabilities = ServerCapabilities(),
        val serverInfo: Implementation,
        val protocolVersion: ProtocolVersion = LATEST_VERSION,
        override val _meta: Map<String, Any> = default,
    ) : HasMeta, ServerMessage.Response

    object Initialized : ClientMessage.Request, HasMethod {
        override val Method = of("notifications/initialized")
    }
}
