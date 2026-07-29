/*
 * Copyright (c) 2025-present http4k Ltd. All rights reserved.
 * Licensed under the http4k Commercial License: https://http4k.org/commercial-license
 */
package org.http4k.ai.mcp

import org.http4k.ai.mcp.model.Meta
import org.http4k.ai.mcp.model.Meta.Companion.default
import org.http4k.ai.mcp.protocol.ClientCapabilities
import org.http4k.ai.mcp.protocol.ClientCapabilities.Companion.All
import org.http4k.ai.mcp.protocol.ProtocolVersion
import org.http4k.ai.mcp.protocol.ProtocolVersion.Companion.LATEST_VERSION
import org.http4k.ai.mcp.protocol.ServerCapabilities
import org.http4k.ai.mcp.protocol.VersionedMcpEntity
import org.http4k.core.Request

/**
 *  Processes an initialize request from an MCP server to a client
 */
typealias InitializeHandler = (InitializeRequest) -> InitializeResponse

fun interface InitializeFilter {
    operator fun invoke(handler: InitializeHandler): InitializeHandler

    companion object
}

data class InitializeRequest(
    val clientInfo: VersionedMcpEntity,
    val capabilities: ClientCapabilities = All,
    val protocolVersion: ProtocolVersion = LATEST_VERSION,
    override val meta: Meta = default,
    val connectRequest: Request? = null
) : CapabilityRequest

sealed interface InitializeResponse {
    data class Ok(
        val serverInfo: VersionedMcpEntity,
        val capabilities: ServerCapabilities = ServerCapabilities(),
        val protocolVersion: ProtocolVersion = LATEST_VERSION,
        val instructions: String? = null,
        val _meta: Meta = default,
    ) : InitializeResponse

    data class Error(val message: String) : InitializeResponse
}
