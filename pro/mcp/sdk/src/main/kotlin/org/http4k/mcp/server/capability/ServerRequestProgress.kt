package org.http4k.mcp.server.capability

import org.http4k.mcp.model.Progress
import org.http4k.mcp.protocol.messages.McpProgress
import org.http4k.mcp.server.protocol.ClientRequestContext
import org.http4k.mcp.server.protocol.RequestProgress
import java.util.concurrent.ConcurrentHashMap

class ServerRequestProgress : RequestProgress {

    private val callbacks = ConcurrentHashMap<ClientRequestContext, (Progress) -> Unit>()

    override fun onProgress(context: ClientRequestContext, handler: (McpProgress.Notification) -> Unit) {
        callbacks[context] = {
            handler(McpProgress.Notification(it.progress, it.total, it.progressToken))
        }
    }

    override fun remove(context: ClientRequestContext) {
        callbacks.remove(context)
    }

    override fun report(req: Progress) {
        callbacks.forEach {
            it.value(req)
        }
    }
}
