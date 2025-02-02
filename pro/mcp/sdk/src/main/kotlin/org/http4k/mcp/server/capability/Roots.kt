package org.http4k.mcp.server.capability

import org.http4k.mcp.model.CompletionStatus
import org.http4k.mcp.model.CompletionStatus.Finished
import org.http4k.mcp.model.Root
import org.http4k.mcp.protocol.messages.McpRoot
import org.http4k.mcp.util.ObservableList

/**
 * Handles protocol traffic for client provided roots.
 */
class Roots : ObservableList<Root>(emptyList()) {
    fun update(req: McpRoot.List.Response): CompletionStatus {
        items = req.roots
        return Finished
    }
}
