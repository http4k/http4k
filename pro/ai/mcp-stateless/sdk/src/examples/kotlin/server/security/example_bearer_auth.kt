/*
 * Copyright (c) 2025-present http4k Ltd. All rights reserved.
 * Licensed under the http4k Commercial License: https://http4k.org/commercial-license
 */
package server.security

import org.http4k.ai.mcp.model.McpEntity
import org.http4k.ai.mcp.protocol.ServerMetaData
import org.http4k.ai.mcp.protocol.Version
import org.http4k.ai.mcp.server.security.BearerAuthMcpSecurity
import org.http4k.filter.debug
import org.http4k.routing.mcp
import org.http4k.server.JettyLoom
import org.http4k.server.asServer

/**
 * This example demonstrates how to secure an MCP server with Bearer Auth built into the server.
 */
fun main() {
    val secureMcpServer = mcp(
        ServerMetaData(McpEntity.of("http4k mcp server"), Version.of("0.1.0")),
        BearerAuthMcpSecurity { it == "token" }
    )

    secureMcpServer
        .debug(debugStream = true)
        .asServer(JettyLoom(3001))
        .start()
}
