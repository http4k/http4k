package org.http4k.mcp.server.capability

import org.http4k.mcp.model.Progress
import org.http4k.mcp.protocol.messages.McpProgress
import org.http4k.mcp.server.protocol.ClientRequestTarget
import org.http4k.mcp.server.protocol.RequestProgress

class ServerRequestProgress : ClientTracking<ClientRequestTarget, (Progress) -> Unit>(), RequestProgress {
    override fun onProgress(target: ClientRequestTarget, handler: (McpProgress.Notification) -> Unit) {
        add(target) { handler(McpProgress.Notification(it.progress, it.total, it.progressToken)) }
    }

    override fun report(target: ClientRequestTarget, req: Progress) {
        subscriptions[target]?.invoke(req)
    }
}
