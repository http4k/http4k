package org.http4k.mcp.protocol

import org.http4k.mcp.protocol.HasMeta.Companion.default
import org.http4k.mcp.protocol.McpRpcMethod.Companion.of
import org.http4k.mcp.protocol.ProtocolVersion.Companion.LATEST_VERSION
import se.ansman.kotshi.JsonSerializable

object McpInitialize : HasMethod {
    override val Method = McpRpcMethod.of("initialize")

    @JsonSerializable
    data class Request(
        val clientInfo: McpEntity,
        val capabilities: ClientCapabilities = ClientCapabilities(),
        val protocolVersion: ProtocolVersion = LATEST_VERSION
    ) : ClientMessage.Request

    @JsonSerializable
    data class Response(
        val serverInfo: McpEntity,
        val capabilities: ServerCapabilities = ServerCapabilities(),
        val protocolVersion: ProtocolVersion = LATEST_VERSION,
        override val _meta: Map<String, Any> = default,
    ) : HasMeta, ServerMessage.Response

    @JsonSerializable
    data object Initialized : ClientMessage.Notification, HasMethod {
        override val Method = of("notifications/initialized")
    }
}
