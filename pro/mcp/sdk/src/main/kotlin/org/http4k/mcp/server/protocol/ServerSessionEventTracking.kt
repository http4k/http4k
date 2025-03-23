package org.http4k.mcp.server.protocol

import org.http4k.mcp.protocol.SessionId
import org.http4k.sse.SseEventId
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicInteger

class ServerSessionEventTracking : SessionEventTracking {
    private val sessionEventTracking = ConcurrentHashMap<SessionId, AtomicInteger>()

    override fun next(sessionId: SessionId) =
        SseEventId(sessionEventTracking.getOrPut(sessionId) { AtomicInteger(0) }.incrementAndGet().toString())

    override fun remove(sessionId: SessionId) {
        sessionEventTracking.remove(sessionId)
    }
}
