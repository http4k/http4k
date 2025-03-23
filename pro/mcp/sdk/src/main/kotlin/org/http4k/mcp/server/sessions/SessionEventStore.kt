package org.http4k.mcp.server.sessions

import org.http4k.mcp.server.protocol.Session
import org.http4k.sse.SseEventId
import org.http4k.sse.SseMessage

/**
 * Cache for server-sent events for a session, facilitating replay of events when a session reconnects
 */
interface SessionEventStore {
    fun read(session: Session, lastEventId: SseEventId?): Sequence<SseMessage.Event>
    fun write(session: Session, message: SseMessage.Event)

    companion object {
        object NoCache : SessionEventStore {
            override fun read(session: Session, lastEventId: SseEventId?) = emptySequence<SseMessage.Event>()

            override fun write(session: Session, message: SseMessage.Event) {}
        }
    }
}
