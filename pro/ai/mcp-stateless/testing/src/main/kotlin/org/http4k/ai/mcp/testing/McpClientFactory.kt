/*
 * Copyright (c) 2025-present http4k Ltd. All rights reserved.
 * Licensed under the http4k Commercial License: https://http4k.org/commercial-license
 */
package org.http4k.ai.mcp.testing

import org.http4k.ai.mcp.client.McpClient
import org.http4k.ai.mcp.client.http.HttpStreamingMcpClient
import org.http4k.client.JavaHttpClient
import org.http4k.core.BodyMode
import org.http4k.core.HttpHandler
import org.http4k.core.Method.POST
import org.http4k.core.PolyHandler
import org.http4k.core.Request
import org.http4k.core.Uri

/**
 * Easy MCP Client creation for testing purposes.
 */
fun interface McpClientFactory : () -> McpClient {

    companion object {
        /**
         * Returns an McpClient connected to the given remote server.
         */
        fun Http(
            serverUri: Uri,
            http: HttpHandler = JavaHttpClient(responseBodyMode = BodyMode.Stream)
        ) =
            McpClientFactory {
                HttpStreamingMcpClient(
                    serverUri,
                    http = http
                )
            }

        /**
         * Creates an in-memory MCP server and returns an McpClient connected to it.
         */
        fun Test(mcpServer: PolyHandler) = McpClientFactory { mcpServer.testMcpClient(Request(POST, "/mcp")) }
    }
}
