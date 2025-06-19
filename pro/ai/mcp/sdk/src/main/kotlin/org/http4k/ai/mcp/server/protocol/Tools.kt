package org.http4k.ai.mcp.server.protocol

import org.http4k.core.Request
import org.http4k.ai.mcp.Client
import org.http4k.ai.mcp.protocol.messages.McpTool

/**
 * Handles protocol traffic for server provided tools.
 */
interface Tools {
    fun list(req: McpTool.List.Request, client: Client, http: Request): McpTool.List.Response

    fun call(req: McpTool.Call.Request, client: Client, http: Request): McpTool.Call.Response
}

