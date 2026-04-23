/*
 * Copyright (c) 2025-present http4k Ltd. All rights reserved.
 * Licensed under the http4k Commercial License: https://http4k.org/commercial-license
 */
package org.http4k.ai.mcp.protocol.messages

import org.http4k.ai.mcp.model.Meta
import org.http4k.ai.mcp.protocol.ClientCapabilities
import org.http4k.ai.mcp.protocol.ClientCapabilities.Companion.All
import org.http4k.ai.mcp.protocol.McpRpcMethod
import org.http4k.ai.mcp.protocol.ProtocolVersion
import org.http4k.ai.mcp.protocol.ProtocolVersion.Companion.LATEST_VERSION
import org.http4k.ai.mcp.protocol.ServerCapabilities
import org.http4k.ai.mcp.protocol.VersionedMcpEntity
import se.ansman.kotshi.JsonSerializable
import se.ansman.kotshi.PolymorphicLabel

object McpInitialize : McpRpc {
    override val Method = McpRpcMethod.of("initialize")

    @JsonSerializable
    @PolymorphicLabel("initialize")
    data class Request(val params: Params, override val id: Any?, val jsonrpc: String = "2.0") : McpJsonRpcRequest() {
        override val method = McpInitialize.Method

        @JsonSerializable
        data class Params(
            val clientInfo: VersionedMcpEntity,
            val capabilities: ClientCapabilities = All,
            val protocolVersion: ProtocolVersion = LATEST_VERSION,
            override val _meta: Meta = Meta.default,
        ) : ClientMessage.Request
    }

    @JsonSerializable
    data class Response(val result: Result, override val id: Any?, val jsonrpc: String = "2.0") : McpJsonRpcResponse() {
        @JsonSerializable
        data class Result(
            val serverInfo: VersionedMcpEntity,
            val capabilities: ServerCapabilities = ServerCapabilities(),
            val protocolVersion: ProtocolVersion = LATEST_VERSION,
            val instructions: String? = null,
            override val _meta: Meta = Meta.default,
        ) : HasMeta, ServerMessage.Response
    }

    data object Initialized : McpRpc {
        override val Method = McpRpcMethod.of("notifications/initialized")

        @JsonSerializable
        @PolymorphicLabel("notifications/initialized")
        data class Notification(val params: Params, override val id: Any? = null, val jsonrpc: String = "2.0") : McpJsonRpcRequest() {
            override val method = Initialized.Method

            @JsonSerializable
            data class Params(
                override val _meta: Meta = Meta.default
            ) : ClientMessage.Notification
        }
    }
}
