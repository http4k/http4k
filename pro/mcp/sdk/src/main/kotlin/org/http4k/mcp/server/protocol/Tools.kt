package org.http4k.mcp.server.protocol

import org.http4k.core.Request
import org.http4k.mcp.protocol.messages.McpTool

/**
 * Handles protocol traffic for server provided tools.
 */
interface Tools {
    fun list(req: McpTool.List.Request, http: Request): McpTool.List.Response

    fun call(req: McpTool.Call.Request, http: Request, client: Client): McpTool.Call.Response

    fun onChange(session: Session, handler: () -> Any)

    fun remove(session: Session)
}

