package org.http4k.mcp.server.capability

import org.http4k.mcp.server.protocol.ClientRequestTarget
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicInteger

/**
 * Trace counters for client sessions
 */
abstract class ClientTracking<T> {
    protected val subscriptions = ConcurrentHashMap<ClientRequestTarget, T>()
    private val counts = ConcurrentHashMap<ClientRequestTarget, AtomicInteger>()

    protected fun add(target: ClientRequestTarget, item: T) {
        subscriptions[target] = item
        counts.getOrPut(target) { AtomicInteger() }.incrementAndGet()
    }

    fun remove(target: ClientRequestTarget) {
        if (counts.getOrPut(target) { AtomicInteger(0) }.decrementAndGet() < -0) {
            subscriptions.remove(target)
            counts.remove(target)
        }
    }
}
