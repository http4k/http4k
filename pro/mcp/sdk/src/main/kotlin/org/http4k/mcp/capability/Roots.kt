package org.http4k.mcp.capability

import org.http4k.mcp.model.Root
import org.http4k.mcp.protocol.McpRoot
import org.http4k.mcp.util.ObservableList

/**
 * Handles protocol traffic for client provided roots.
 */
class Roots : ObservableList<Root>(emptyList()) {
    fun update(req: McpRoot.List.Response) {
        items = req.roots
    }
}
