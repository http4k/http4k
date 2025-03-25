package org.http4k.mcp.server.protocol

import org.http4k.mcp.model.Progress
import org.http4k.mcp.protocol.messages.McpProgress

/**
 * Handles protocol traffic for client progress notifications.
 */
interface RequestProgress {
    fun onProgress(context: ClientRequestContext, handler: (McpProgress.Notification) -> Unit)
    fun remove(target: ClientRequestContext)
    fun report(req: Progress)
}
