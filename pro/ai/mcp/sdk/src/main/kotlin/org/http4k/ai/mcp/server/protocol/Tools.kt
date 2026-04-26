/*
 * Copyright (c) 2025-present http4k Ltd. All rights reserved.
 * Licensed under the http4k Commercial License: https://http4k.org/commercial-license
 */
package org.http4k.ai.mcp.server.protocol

import org.http4k.ai.mcp.Client
import org.http4k.ai.mcp.ToolHandler
import org.http4k.ai.mcp.protocol.messages.McpTool
import org.http4k.ai.mcp.server.capability.ToolCapability
import org.http4k.ai.model.ToolName
import org.http4k.core.Request

/**
 * Handles protocol traffic for server provided tools.
 */
interface Tools : ObservableCapability<ToolCapability>, Iterable<ToolCapability>, (ToolName) -> ToolHandler {
    fun list(req: McpTool.List.Request.Params, client: Client, http: Request): McpTool.List.Response.Result

    fun call(req: McpTool.Call.Request.Params, client: Client, http: Request): McpTool.Call.Response.Result
}

