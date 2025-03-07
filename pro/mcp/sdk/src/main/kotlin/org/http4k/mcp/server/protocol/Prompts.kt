package org.http4k.mcp.server.protocol

import org.http4k.core.Request
import org.http4k.mcp.protocol.SessionId
import org.http4k.mcp.protocol.messages.McpPrompt

/**
 * Handles protocol traffic for prompts features.
 */
interface Prompts {
    fun get(req: McpPrompt.Get.Request, http: Request): McpPrompt.Get.Response
    fun list(mcp: McpPrompt.List.Request, http: Request): McpPrompt.List.Response
    fun onChange(sessionId: SessionId, handler: () -> Any)
    fun remove(sessionId: SessionId)
}
