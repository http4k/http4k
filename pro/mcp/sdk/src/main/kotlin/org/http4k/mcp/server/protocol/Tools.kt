package org.http4k.mcp.server.protocol

import org.http4k.core.Request
import org.http4k.mcp.protocol.SessionId
import org.http4k.mcp.protocol.messages.McpTool

/**
 * Handles protocol traffic for server provided tools.
 */
interface Tools {
    fun list(req: McpTool.List.Request, http: Request): McpTool.List.Response

    fun call(req: McpTool.Call.Request, http: Request): McpTool.Call.Response

    fun onChange(sessionId: SessionId, handler: () -> Any)

    fun remove(sessionId: SessionId)
}

