package org.http4k.mcp.server.capability

import org.http4k.mcp.model.Progress
import org.http4k.mcp.protocol.messages.McpProgress
import org.http4k.mcp.server.protocol.ClientRequestMethod
import org.http4k.mcp.server.protocol.RequestProgress
import java.util.concurrent.ConcurrentHashMap

class ServerRequestProgress : RequestProgress {

    private val callbacks = ConcurrentHashMap<ClientRequestMethod, (Progress) -> Unit>()

    override fun onProgress(method: ClientRequestMethod, handler: (McpProgress.Notification) -> Unit) {
        callbacks[method] = {
            handler(McpProgress.Notification(it.progress, it.total, it.progressToken))
        }
    }

    override fun remove(method: ClientRequestMethod) {
        callbacks.remove(method)
    }

    override fun report(req: Progress) {
        callbacks.forEach {
            it.value(req)
        }
    }
}
