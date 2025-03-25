package org.http4k.mcp.server.protocol

import org.http4k.mcp.model.Progress
import org.http4k.mcp.protocol.messages.McpProgress

/**
 * Handles protocol traffic for client progress notifications.
 */
interface RequestProgress {
    fun onProgress(method: ClientRequestMethod, handler: (McpProgress.Notification) -> Unit)
    fun remove(method: ClientRequestMethod)
    fun report(req: Progress)
}
