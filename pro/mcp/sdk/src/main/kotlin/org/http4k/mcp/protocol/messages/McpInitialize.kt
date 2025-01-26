package org.http4k.mcp.protocol.messages

import org.http4k.mcp.protocol.ClientCapabilities
import org.http4k.mcp.protocol.McpRpcMethod
import org.http4k.mcp.protocol.ProtocolVersion
import org.http4k.mcp.protocol.ProtocolVersion.Companion.LATEST_VERSION
import org.http4k.mcp.protocol.ServerCapabilities
import org.http4k.mcp.protocol.VersionedMcpEntity
import org.http4k.mcp.protocol.messages.HasMeta.Companion.default
import se.ansman.kotshi.JsonSerializable

object McpInitialize : HasMethod {
    override val Method = McpRpcMethod.of("initialize")

    @JsonSerializable
    data class Request(
        val clientInfo: VersionedMcpEntity,
        val capabilities: ClientCapabilities = ClientCapabilities(),
        val protocolVersion: ProtocolVersion = LATEST_VERSION
    ) : ClientMessage.Request

    @JsonSerializable
    data class Response(
        val serverInfo: VersionedMcpEntity,
        val capabilities: ServerCapabilities = ServerCapabilities(),
        val protocolVersion: ProtocolVersion = LATEST_VERSION,
        override val _meta: Map<String, Any> = default,
    ) : HasMeta, ServerMessage.Response

    @JsonSerializable
    data class Initialized(
        override val method: McpRpcMethod = Method
    ) : ClientMessage.Notification {
        companion object : HasMethod {
            override val Method = McpRpcMethod.of("notifications/initialized")
        }
    }
}
