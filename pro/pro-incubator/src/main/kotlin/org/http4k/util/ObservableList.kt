package org.http4k.util

import org.http4k.mcp.SessionId
import kotlin.properties.Delegates

abstract class ObservableList<T>(initial: List<T>) : Iterable<T> {
    private val handlers = mutableMapOf<SessionId, () -> Any>()

    var items by Delegates.observable(initial) { _, _, _ -> handlers.values.forEach { it() } }

    fun onChange(sessionId: SessionId, handler: () -> Any) {
        handlers[sessionId]= handler
    }

    fun remove(sessionId: SessionId) {
        handlers.remove(sessionId)
    }

    override fun iterator() = items.iterator()
}
