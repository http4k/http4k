package org.http4k.mcp.server.protocol

import org.http4k.mcp.protocol.SessionId
import org.http4k.sse.SseEventId

/**
 * Tracks the next event ID for a given session.
 */
interface SessionEventTracking {
    fun next(sessionId: SessionId): SseEventId
    fun remove(sessionId: SessionId)
}
