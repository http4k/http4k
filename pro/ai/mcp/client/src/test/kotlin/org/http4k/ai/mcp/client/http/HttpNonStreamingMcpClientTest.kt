/*
 * Copyright (c) 2025-present http4k Ltd. All rights reserved.
 * Licensed under the http4k Commercial License: https://http4k.org/commercial-license
 */
package org.http4k.ai.mcp.client.http

import org.http4k.ai.mcp.client.McpClientContract
import org.http4k.ai.mcp.model.McpEntity
import org.http4k.ai.mcp.protocol.Version
import org.http4k.ai.mcp.server.http.HttpNonStreamingMcp
import org.http4k.ai.mcp.server.http.HttpSessions
import org.http4k.ai.mcp.server.protocol.McpProtocol
import org.http4k.ai.mcp.server.security.BasicAuthMcpSecurity
import org.http4k.client.JavaHttpClient
import org.http4k.core.Credentials
import org.http4k.core.Uri
import org.http4k.core.then
import org.http4k.filter.ClientFilters
import org.http4k.routing.poly
import org.http4k.sse.Sse

class HttpNonStreamingMcpClientTest : McpClientContract<Sse>() {

    override val doesNotifications = false

    override fun clientSessions() = HttpSessions(
        sessionProvider,
        sessionEventTracking,
        sessionEventStore,
    )

    private val creds = Credentials("user", "password")

    override fun clientFor(port: Int) = HttpNonStreamingMcpClient(
        Uri.of("http://localhost:${port}/mcp"),
        McpEntity.of("http4k MCP client"), Version.of("0.0.0"),
        ClientFilters.BasicAuth(creds).then(JavaHttpClient()),
    )

    override fun toPolyHandler(protocol: McpProtocol<Sse>) =
        poly(HttpNonStreamingMcp(protocol, BasicAuthMcpSecurity("", { creds == it })))
}
