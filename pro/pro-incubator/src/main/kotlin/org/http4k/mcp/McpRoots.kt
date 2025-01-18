package org.http4k.mcp

import org.http4k.connect.mcp.McpRoot
import org.http4k.connect.mcp.model.Root
import org.http4k.util.ObservableList

class McpRoots : ObservableList<Root>(emptyList()) {
    fun update(req: McpRoot.List.Response) {
        items = req.roots
    }
}
