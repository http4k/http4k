package org.http4k.connect.mcp

import org.http4k.jsonrpc.jsonRpcVersion

data class ConnectionResponse(
    val serverInfo: Implementation,
    val capabilities: ServerCapabilities = ServerCapabilities(),
    val protocolVersion: String = jsonRpcVersion,
    val _meta: Unit? = null,
)
