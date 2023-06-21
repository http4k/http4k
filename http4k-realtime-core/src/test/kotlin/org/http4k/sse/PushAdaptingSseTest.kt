package org.http4k.sse

import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.equalTo
import org.http4k.core.Method
import org.http4k.core.Request
import org.junit.jupiter.api.Test
import java.util.concurrent.atomic.AtomicBoolean

class PushAdaptingSseTest {

    class TestAdapter : PushAdaptingSse() {
        val received = mutableListOf<SseMessage>()
        var closed = AtomicBoolean(false)

        override fun send(message: SseMessage) {
            received += message
        }

        override fun close() {
            closed.set(true)
        }
    }

    @Test
    fun `outbound comms are pushed to client`() {
        val adapter = TestAdapter()
        adapter.send(SseMessage.Data("hello"))
        assertThat(adapter.received, equalTo(listOf(SseMessage.Data("hello"))))
        adapter.close()
        assertThat(adapter.closed.get(), equalTo(true))
    }
}
