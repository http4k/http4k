package org.http4k.mcp.server.capability

import org.http4k.mcp.model.Progress
import org.http4k.mcp.protocol.messages.McpProgress
import org.http4k.mcp.server.protocol.RequestProgress
import org.http4k.mcp.server.protocol.Session
import java.util.concurrent.ConcurrentHashMap

class ServerRequestProgress : RequestProgress {

    private val callbacks = ConcurrentHashMap<Session, (Progress) -> Unit>()

    override fun onProgress(session: Session, handler: (McpProgress.Notification) -> Unit) {
        callbacks[session] = {
            handler(McpProgress.Notification(it.progress, it.total, it.progressToken))
        }
    }

    override fun remove(session: Session) {
        callbacks.remove(session)
    }

    override fun report(req: Progress) {
        callbacks.forEach {
            it.value(req)
        }
    }
}
