package org.http4k.websocket

import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.equalTo
import org.http4k.core.Method.GET
import org.http4k.core.Request
import org.http4k.websocket.WsStatus.Companion.NEVER_CONNECTED
import org.junit.jupiter.api.Test
import java.util.concurrent.atomic.AtomicReference

class PushPullAdaptingWebSocketTest {

    class TestAdapter : PushPullAdaptingWebSocket(Request(GET, "/bob")) {
        val received = mutableListOf<WsMessage>()
        val closed = AtomicReference<WsStatus>(null)

        override fun send(message: WsMessage) {
            received += message
        }

        override fun close(status: WsStatus) {
            closed.set(status)
        }
    }

    @Test
    fun `outbound comms are pushed to client`() {
        val adapter = TestAdapter()
        adapter.send(WsMessage("hello"))
        assertThat(adapter.received, equalTo(listOf(WsMessage("hello"))))
        adapter.close(NEVER_CONNECTED)
        assertThat(adapter.closed.get(), equalTo(NEVER_CONNECTED))
    }

    @Test
    fun `inbound comms are pushed to socket`() {
        val outboundClose = AtomicReference<WsStatus>(null)
        val outboundMessage = AtomicReference<WsMessage>(null)
        val outboundError = AtomicReference<Throwable>(null)
        val adapter = TestAdapter().apply {
            onMessage {
                outboundMessage.set(it)
            }
            onError {
                outboundError.set(it)
            }
            onClose {
                outboundClose.set(it)
            }
        }

        adapter.triggerMessage(WsMessage("hello"))
        assertThat(outboundMessage.get(), equalTo(WsMessage("hello")))

        val throwable: Throwable = RuntimeException("foo")
        adapter.triggerError(throwable)
        assertThat(outboundError.get(), equalTo(throwable))

        adapter.triggerClose(NEVER_CONNECTED)
        assertThat(outboundClose.get(), equalTo(NEVER_CONNECTED))
    }

}
