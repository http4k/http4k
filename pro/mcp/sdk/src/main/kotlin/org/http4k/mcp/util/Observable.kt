package org.http4k.mcp.util

import org.http4k.mcp.server.protocol.Session
import java.util.concurrent.ConcurrentHashMap

abstract class Observable {
    protected val callbacks = ConcurrentHashMap<Session, () -> Any>()

    fun onChange(session: Session, handler: () -> Any) {
        callbacks[session] = handler
    }

    open fun remove(session: Session) {
        callbacks.remove(session)
    }
}
