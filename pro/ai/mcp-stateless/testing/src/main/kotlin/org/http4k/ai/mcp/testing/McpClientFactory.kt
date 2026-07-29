/*
 * Copyright (c) 2025-present http4k Ltd. All rights reserved.
 * Licensed under the http4k Commercial License: https://http4k.org/commercial-license
 */
package org.http4k.ai.mcp.testing

import org.http4k.ai.mcp.client.McpClient
import org.http4k.ai.mcp.client.http.HttpMcpClient
import org.http4k.client.JavaHttpClient
import org.http4k.core.HttpHandler
import org.http4k.core.Uri

/**
 * Easy MCP Client creation for testing purposes.
 */
fun interface McpClientFactory : () -> McpClient {

    companion object {
        fun Http(serverUri: Uri, http: HttpHandler = JavaHttpClient()) =
            McpClientFactory { HttpMcpClient(serverUri, http = http) }

        /** In-memory MCP server handler -> connected client. */
        fun Test(mcpServer: HttpHandler) = McpClientFactory { mcpServer.testMcpClient() }
    }
}
