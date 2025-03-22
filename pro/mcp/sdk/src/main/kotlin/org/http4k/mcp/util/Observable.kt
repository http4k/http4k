package org.http4k.mcp.util

import org.http4k.mcp.protocol.SessionId
import java.util.concurrent.ConcurrentHashMap

abstract class Observable {
    protected val callbacks = ConcurrentHashMap<SessionId, () -> Any>()

    fun onChange(sessionId: SessionId, handler: () -> Any) {
        callbacks[sessionId] = handler
    }

    open fun remove(sessionId: SessionId) {
        callbacks.remove(sessionId)
    }
}
