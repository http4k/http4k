package org.http4k.mcp.server.protocol

import org.http4k.mcp.model.Progress
import org.http4k.mcp.protocol.messages.McpProgress

/**
 * Handles protocol traffic for client progress notifications.
 */
interface RequestProgress {
    fun onProgress(target: ClientRequestTarget, handler: (McpProgress.Notification) -> Unit)
    fun remove(target: ClientRequestTarget)
    fun report(target: ClientRequestTarget, req: Progress)
}
