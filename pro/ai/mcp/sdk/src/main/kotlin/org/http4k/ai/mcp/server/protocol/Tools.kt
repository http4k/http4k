/*
 * Copyright (c) 2025-present http4k Ltd. All rights reserved.
 * Licensed under the http4k Commercial License: https://http4k.org/commercial-license
 */
package org.http4k.ai.mcp.server.protocol

import org.http4k.ai.mcp.Client
import org.http4k.ai.mcp.protocol.messages.McpTool
import org.http4k.core.Request

/**
 * Handles protocol traffic for server provided tools.
 */
interface Tools {
    fun list(req: McpTool.List.Request, client: Client, http: Request): McpTool.List.Response

    fun call(req: McpTool.Call.Request, client: Client, http: Request): McpTool.Call.Response
}

