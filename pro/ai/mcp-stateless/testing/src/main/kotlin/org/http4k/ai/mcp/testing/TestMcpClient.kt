/*
 * Copyright (c) 2025-present http4k Ltd. All rights reserved.
 * Licensed under the http4k Commercial License: https://http4k.org/commercial-license
 */
package org.http4k.ai.mcp.testing

import org.http4k.ai.mcp.client.McpClient
import org.http4k.ai.mcp.client.http.HttpMcpClient
import org.http4k.core.HttpHandler
import org.http4k.core.Uri

/**
 * In-memory MCP test client: the stateless client wired straight to the server handler (no network).
 */
fun HttpHandler.testMcpClient(baseUri: Uri = Uri.of("http://mcp/mcp")): McpClient = HttpMcpClient(baseUri, http = this)
