package org.http4k.mcp.server.sessions

import org.http4k.mcp.protocol.SessionId
import org.http4k.sse.SseEventId
import org.http4k.sse.SseMessage

/**
 * Cache for server-sent events for a session, facilitating replay of events when a session reconnects
 */
interface SessionEventStore {
    fun read(sessionId: SessionId, lastEventId: SseEventId?): Sequence<SseMessage.Event>
    fun write(sessionId: SessionId, message: SseMessage.Event)

    companion object {
        object NoCache : SessionEventStore {
            override fun read(sessionId: SessionId, lastEventId: SseEventId?) = emptySequence<SseMessage.Event>()

            override fun write(sessionId: SessionId, message: SseMessage.Event) {}
        }
    }
}
