package org.http4k.mcp.server

import org.http4k.mcp.model.Root
import org.http4k.mcp.protocol.McpRoot
import org.http4k.util.ObservableList

class McpRoots : ObservableList<Root>(emptyList()) {
    fun update(req: McpRoot.List.Response) {
        items = req.roots
    }
}
