package org.http4k.mcp.util

import org.http4k.mcp.server.protocol.Session
import org.http4k.mcp.server.protocol.ObservableCapability
import java.util.concurrent.ConcurrentHashMap

abstract class Observable : ObservableCapability {
    protected val callbacks = ConcurrentHashMap<Session, () -> Any>()

    override fun onChange(session: Session, handler: () -> Any) {
        callbacks[session] = handler
    }

    override fun remove(session: Session) {
        callbacks.remove(session)
    }
}
