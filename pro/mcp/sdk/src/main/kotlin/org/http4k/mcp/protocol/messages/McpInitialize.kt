package org.http4k.mcp.protocol.messages

import org.http4k.mcp.model.Meta
import org.http4k.mcp.protocol.ClientCapabilities
import org.http4k.mcp.protocol.ClientCapabilities.Companion.All
import org.http4k.mcp.protocol.McpRpcMethod
import org.http4k.mcp.protocol.McpRpcMethod.Companion.of
import org.http4k.mcp.protocol.ProtocolVersion
import org.http4k.mcp.protocol.ProtocolVersion.Companion.LATEST_VERSION
import org.http4k.mcp.protocol.ServerCapabilities
import org.http4k.mcp.protocol.VersionedMcpEntity
import se.ansman.kotshi.JsonSerializable

object McpInitialize : McpRpc {
    override val Method = McpRpcMethod.of("initialize")

    @JsonSerializable
    data class Request(
        val clientInfo: VersionedMcpEntity,
        val capabilities: ClientCapabilities = All,
        val protocolVersion: ProtocolVersion = LATEST_VERSION
    ) : ClientMessage.Request

    @JsonSerializable
    data class Response(
        val serverInfo: VersionedMcpEntity,
        val capabilities: ServerCapabilities = ServerCapabilities(),
        val protocolVersion: ProtocolVersion = LATEST_VERSION,
        override val _meta: Meta = Meta.default,
    ) : HasMeta, ServerMessage.Response

    data object Initialized : McpRpc {
        override val Method = of("notifications/initialized")

        @JsonSerializable
        data object Notification : ClientMessage.Notification
    }
}
