/*
 * Copyright (c) 2025-present http4k Ltd. All rights reserved.
 * Licensed under the http4k Commercial License: https://http4k.org/commercial-license
 */
package org.http4k.ai.mcp.client.http

import org.http4k.ai.mcp.protocol.McpRpcMethod
import org.http4k.core.Filter
import org.http4k.core.with
import org.http4k.lens.Header
import org.http4k.lens.MCP_METHOD
import org.http4k.lens.MCP_NAME

fun PopulateMcpHeaders(method: McpRpcMethod, name: String) = Filter { next ->
    { request -> next(request.with(Header.MCP_METHOD of method).with(Header.MCP_NAME of name)) }
}
