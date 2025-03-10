package org.http4k.mcp.util

import org.http4k.mcp.protocol.SessionId
import java.util.concurrent.ConcurrentHashMap
import kotlin.properties.Delegates

abstract class ObservableList<T>(initial: Iterable<T>) : Iterable<T> {
    private val callbacks = ConcurrentHashMap<SessionId, () -> Any>()

    var items by Delegates.observable(initial) { _, _, _ -> callbacks.values.forEach { it() } }

    fun onChange(sessionId: SessionId, handler: () -> Any) {
        callbacks[sessionId] = handler
    }

    open fun remove(sessionId: SessionId) {
        callbacks.remove(sessionId)
    }

    override fun iterator() = items.iterator()
}
