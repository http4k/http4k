package org.http4k.ai.mcp.server.sessions

import org.http4k.ai.mcp.server.protocol.Session
import org.http4k.sse.SseEventId
import org.http4k.sse.SseMessage
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.CopyOnWriteArrayList

/**
 * Cache for server-sent events for a session, facilitating replay of events when a session reconnects
 */
interface SessionEventStore {
    fun read(session: Session, lastEventId: SseEventId?): Sequence<SseMessage.Event>
    fun write(session: Session, message: SseMessage.Event)

    companion object {
        /**
         * No cache for server-sent events for a session, no replay of events when a session reconnects
         */
        object NoCache : SessionEventStore {
            override fun read(session: Session, lastEventId: SseEventId?) = emptySequence<SseMessage.Event>()

            override fun write(session: Session, message: SseMessage.Event) {}
        }

        /**
         * In-memory cache for server-sent events for a session, facilitating replay of most recent events when a session reconnects.
         * Configurable size of cache.
         */
        fun InMemory(memorySize: Int) = object : SessionEventStore {
            private val sessionEvents = ConcurrentHashMap<Session, CopyOnWriteArrayList<SseMessage.Event>>()

            override fun read(session: Session, lastEventId: SseEventId?) =
                when (val lastEventIdInt = lastEventId?.value?.toIntOrNull()) {
                    null -> sessionEvents[session]?.asSequence() ?: emptySequence()
                    else -> sessionEvents[session]?.asSequence()
                        ?.dropWhile { it.id!!.value.toInt() <= lastEventIdInt }
                        ?: emptySequence()
                }

            override fun write(session: Session, message: SseMessage.Event) {
                if (message.event == "message" && message.id != null) {
                    val events = sessionEvents.computeIfAbsent(session) { CopyOnWriteArrayList() }
                    if (events.size >= memorySize) events.removeAt(0)
                    events.add(message)
                }
            }

            override fun toString() = "InMemory(sessionEvents=$sessionEvents)"
        }
    }
}
