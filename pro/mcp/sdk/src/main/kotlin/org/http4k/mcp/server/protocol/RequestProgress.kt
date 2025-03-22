package org.http4k.mcp.server.protocol

import org.http4k.mcp.model.Progress
import org.http4k.mcp.protocol.SessionId
import org.http4k.mcp.protocol.messages.McpProgress

/**
 * Handles protocol traffic for client progress notifications.
 */
interface RequestProgress {
    fun onProgress(sessionId: SessionId, handler: (McpProgress.Notification) -> Unit)
    fun remove(sessionId: SessionId)
    fun report(req: Progress)
}
