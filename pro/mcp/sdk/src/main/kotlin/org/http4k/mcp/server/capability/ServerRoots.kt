package org.http4k.mcp.server.capability

import org.http4k.mcp.model.CompletionStatus
import org.http4k.mcp.model.CompletionStatus.Finished
import org.http4k.mcp.model.Root
import org.http4k.mcp.protocol.messages.McpRoot
import org.http4k.mcp.server.protocol.Roots
import org.http4k.mcp.util.ObservableList

class ServerRoots : ObservableList<Root>(emptyList()), Roots {
    override fun update(req: McpRoot.List.Response): CompletionStatus {
        items = req.roots
        return Finished
    }
}
