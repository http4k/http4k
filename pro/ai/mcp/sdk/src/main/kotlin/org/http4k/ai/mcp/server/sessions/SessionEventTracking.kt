package org.http4k.ai.mcp.server.sessions

import org.http4k.ai.mcp.server.protocol.Session
import org.http4k.sse.SseEventId
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicInteger

/**
 * Tracks the next event ID for a given session.
 */
interface SessionEventTracking {
    fun next(session: Session): SseEventId
    fun remove(session: Session)

    companion object {
        /**
         * Stores the next event ID in memory.
         */
        fun InMemory() = object : SessionEventTracking {
            private val sessionLastIds = ConcurrentHashMap<Session, AtomicInteger>()

            override fun next(session: Session) =
                SseEventId(sessionLastIds.getOrPut(session) { AtomicInteger(0) }.incrementAndGet().toString())

            override fun remove(session: Session) {
                sessionLastIds.remove(session)
            }

            override fun toString() = "InMemory(sessionLastIds=$sessionLastIds)"
        }
    }
}
