package org.http4k.mcp.server.capability

import org.http4k.mcp.model.Progress
import org.http4k.mcp.protocol.SessionId
import org.http4k.mcp.protocol.messages.McpProgress
import org.http4k.mcp.server.protocol.RequestProgress
import java.util.concurrent.ConcurrentHashMap

class ServerRequestProgress : RequestProgress {

    private val callbacks = ConcurrentHashMap<SessionId, (Progress) -> Unit>()

    override fun onProgress(sessionId: SessionId, handler: (McpProgress.Notification) -> Unit) {
        callbacks[sessionId] = {
            handler(McpProgress.Notification(it.progress, it.total, it.progressToken))
        }
    }

    override fun remove(sessionId: SessionId) {
        callbacks.remove(sessionId)
    }

    override fun report(req: Progress) {
        callbacks.forEach {
            it.value(req)
        }
    }
}
