package org.http4k.mcp.server.sessions

import org.http4k.mcp.protocol.SessionId
import org.http4k.sse.SseEventId
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicInteger

/**
 * Tracks the next event ID for a given session.
 */
interface SessionEventTracking {
    fun next(sessionId: SessionId): SseEventId
    fun remove(sessionId: SessionId)

    companion object {
        /**
         * Stores the next event ID in memory.
         */
        fun InMemory() = object : SessionEventTracking {
            private val sessionLastIds = ConcurrentHashMap<SessionId, AtomicInteger>()

            override fun next(sessionId: SessionId) =
                SseEventId(sessionLastIds.getOrPut(sessionId) { AtomicInteger(0) }.incrementAndGet().toString())

            override fun remove(sessionId: SessionId) {
                sessionLastIds.remove(sessionId)
            }
        }
    }
}
