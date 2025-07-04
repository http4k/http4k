package org.http4k.ai.mcp.server.capability

import org.http4k.ai.mcp.model.CompletionStatus
import org.http4k.ai.mcp.model.CompletionStatus.Finished
import org.http4k.ai.mcp.model.Root
import org.http4k.ai.mcp.protocol.messages.McpRoot
import org.http4k.ai.mcp.server.protocol.Roots
import org.http4k.ai.mcp.util.ObservableList

class ServerRoots : ObservableList<Root>(emptyList()), Roots {
    override fun update(req: McpRoot.List.Response): CompletionStatus {
        items = req.roots
        return Finished
    }
}
