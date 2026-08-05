/*
 * Copyright (c) 2025-present http4k Ltd. All rights reserved.
 * Licensed under the http4k Commercial License: https://http4k.org/commercial-license
 */
package org.http4k.ai.mcp.client.http

import org.http4k.ai.mcp.client.McpClient
import org.http4k.ai.mcp.client.McpClientContract
import org.http4k.ai.mcp.server.http.HttpMcp
import org.http4k.ai.mcp.server.protocol.McpProtocol
import org.http4k.ai.mcp.server.security.NoMcpSecurity
import org.http4k.ai.mcp.testing.testMcpClient

class TestMcpClientTest : McpClientContract() {

    // in-memory testMcpClient drives only the HTTP face (handler.http); it can't reach the SSE subscriptions face
    override val streamsSubscriptions get() = false

    override fun withClient(protocol: McpProtocol, test: McpClient.() -> Unit) {
        val client = HttpMcp(protocol, NoMcpSecurity).http!!.testMcpClient()
        try {
            client.test()
        } finally {
            client.stop()
        }
    }
}
