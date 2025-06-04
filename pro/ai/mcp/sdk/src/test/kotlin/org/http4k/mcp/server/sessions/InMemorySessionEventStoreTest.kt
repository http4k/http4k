package org.http4k.mcp.server.sessions

import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.equalTo
import org.http4k.mcp.protocol.SessionId
import org.http4k.mcp.server.protocol.Session
import org.http4k.sse.SseEventId
import org.http4k.sse.SseMessage
import org.junit.jupiter.api.Test
import java.util.UUID

class InMemorySessionEventStoreTest {
    private val store = SessionEventStore.InMemory(2)
    private val session = Session(SessionId.of(UUID.randomUUID().toString()))
    private val event1 = SseMessage.Event("message", "message", id = SseEventId("1"))
    private val dataMessage = SseMessage.Event("data", "message", id = SseEventId("2"))
    private val event2 = SseMessage.Event("message", "message", id = SseEventId("2"))

    @Test
    fun `returns empty sequence when session does not exist`() {
        assertThat(store.read(Session(SessionId.of("whatever")), null).toList(), equalTo(emptyList()))
    }

    @Test
    fun `does not store events without id`() {
        val eventWithoutId = SseMessage.Event("message", "message")
        store.write(session, eventWithoutId)
        assertThat(store.read(session, null).toList(), equalTo(emptyList()))
    }

    @Test
    fun `returns all events when last event id is not passed`() {
        store.write(session, event1)
        store.write(session, dataMessage)
        store.write(session, event2)

        assertThat(store.read(session, null).toList(), equalTo(listOf(event1, event2)))
    }

    @Test
    fun `returns only events after the last event id`() {
        store.write(session, event1)
        store.write(session, dataMessage)
        store.write(session, event2)

        assertThat(store.read(session, SseEventId("1")).toList(), equalTo(listOf(event2)))
    }

    @Test
    fun `respects memory size limit`() {
        store.write(session, event1)
        store.write(session, dataMessage)
        store.write(session, event2)
        val event3 = SseMessage.Event("message", "message", id = SseEventId("3"))
        store.write(session, event3)

        assertThat(store.read(session, null).toList(), equalTo(listOf(event2, event3)))
    }
}
