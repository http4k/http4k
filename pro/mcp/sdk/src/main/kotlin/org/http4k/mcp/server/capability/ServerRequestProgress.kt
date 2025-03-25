package org.http4k.mcp.server.capability

import org.http4k.mcp.model.Progress
import org.http4k.mcp.protocol.messages.McpProgress
import org.http4k.mcp.server.protocol.ClientRequestContext
import org.http4k.mcp.server.protocol.RequestProgress

class ServerRequestProgress : ClientTracking<ClientRequestContext, (Progress) -> Unit>(), RequestProgress {
    override fun onProgress(context: ClientRequestContext, handler: (McpProgress.Notification) -> Unit) {
        add(context) { handler(McpProgress.Notification(it.progress, it.total, it.progressToken)) }
    }

    override fun report(req: Progress) {
        subscriptions.forEach { it.value(req) }
    }
}
