/*
 * Copyright (c) 2025-present http4k Ltd. All rights reserved.
 * Licensed under the http4k Commercial License: https://http4k.org/commercial-license
 */
package org.http4k.ai.mcp.client.http

import org.http4k.ai.mcp.client.McpClient
import org.http4k.ai.mcp.client.McpClientContract
import org.http4k.ai.mcp.protocol.Version
import org.http4k.ai.mcp.server.http.HttpMcp
import org.http4k.ai.mcp.server.protocol.McpProtocol
import org.http4k.ai.mcp.server.security.OAuthMcpSecurity
import org.http4k.client.JavaHttpClient
import org.http4k.core.Uri
import org.http4k.core.then
import org.http4k.filter.ClientFilters
import org.http4k.server.Helidon
import org.http4k.server.asServer

class HttpMcpClientTest : McpClientContract() {

    private val http = ClientFilters.BearerAuth("123").then(JavaHttpClient())

    override fun withClient(protocol: McpProtocol, test: McpClient.() -> Unit) {
        val server = HttpMcp(protocol, OAuthMcpSecurity(Uri.of("http://auth1"), Uri.of("http://mcp/mcp")) { it == "123" })
            .asServer(Helidon(0)).start()
        val client = HttpMcpClient(Uri.of("http://localhost:${server.port()}/mcp"), clientName, Version.of("1.0.0"), http)
        try {
            client.test()
        } finally {
            client.stop()
            server.stop()
        }
    }
}
