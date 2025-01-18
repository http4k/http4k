package org.http4k.util

import org.http4k.mcp.server.SessionId
import kotlin.properties.Delegates

abstract class ObservableList<T>(initial: List<T>) : Iterable<T> {
    private val callbacks = mutableMapOf<SessionId, () -> Any>()

    var items by Delegates.observable(initial) { _, _, _ -> callbacks.values.forEach { it() } }

    fun onChange(sessionId: SessionId, handler: () -> Any) {
        callbacks[sessionId] = handler
    }

    open fun remove(sessionId: SessionId) {
        callbacks.remove(sessionId)
    }

    override fun iterator() = items.iterator()
}
