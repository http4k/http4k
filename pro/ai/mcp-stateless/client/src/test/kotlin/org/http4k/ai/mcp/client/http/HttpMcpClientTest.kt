/*
 * Copyright (c) 2025-present http4k Ltd. All rights reserved.
 * Licensed under the http4k Commercial License: https://http4k.org/commercial-license
 */
package org.http4k.ai.mcp.client.http

import org.http4k.ai.mcp.client.McpClientContract
import org.http4k.ai.mcp.protocol.Version
import org.http4k.ai.mcp.server.http.HttpMcp
import org.http4k.ai.mcp.server.protocol.McpProtocol
import org.http4k.ai.mcp.server.security.OAuthMcpSecurity
import org.http4k.client.JavaHttpClient
import org.http4k.core.Uri
import org.http4k.core.then
import org.http4k.filter.ClientFilters

class HttpMcpClientTest : McpClientContract() {

    private val http = ClientFilters.BearerAuth("123").then(JavaHttpClient())

    override fun clientFor(port: Int) =
        HttpMcpClient(Uri.of("http://localhost:$port/mcp"), clientName, Version.of("1.0.0"), http)

    override fun toHandler(protocol: McpProtocol) =
        HttpMcp(protocol, OAuthMcpSecurity(Uri.of("http://auth1"), Uri.of("http://mcp/mcp")) { it == "123" })
}
