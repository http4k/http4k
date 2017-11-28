package org.http4k.websocket

import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.equalTo
import org.http4k.core.Method
import org.http4k.core.Request
import org.http4k.core.Status
import org.junit.Test
import java.util.concurrent.atomic.AtomicReference

class PushPullAdaptingWebSocketTest {

    class TestAdapter : PushPullAdaptingWebSocket(Request(Method.GET, "/bob")) {
        val received = mutableListOf<WsMessage>()
        val closed = AtomicReference<Status>(null)

        override fun send(message: WsMessage) {
            received += message
        }

        override fun close(status: Status) {
            closed.set(status)
        }
    }

    @Test
    fun `inbound comms are pushed to client`() {
        val adapter = TestAdapter()
        adapter.send(WsMessage("hello"))
        assertThat(adapter.received, equalTo(listOf(WsMessage("hello"))))
        adapter.close(Status.OK)
        assertThat(adapter.closed.get(), equalTo(Status.OK))
    }

    @Test
    fun `outbound comms are pushed to socket`() {
        val outboundClose = AtomicReference<Status>(null)
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

        adapter.triggerClose(Status.OK)
        assertThat(outboundClose.get(), equalTo(Status.OK))
    }

}