package org.http4k.websocket

import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.equalTo
import org.http4k.core.Method
import org.http4k.core.Request
import org.http4k.testing.testWebsocket
import org.junit.jupiter.api.Test
import java.util.concurrent.atomic.AtomicReference

class WebsocketTest {

    private val message = WsMessage("hello")

    private class TestConsumer : WsConsumer {
        lateinit var websocket: Websocket
        val messages = mutableListOf<WsMessage>()
        val throwable = mutableListOf<Throwable>()
        val closed = AtomicReference<WsStatus>()

        override fun invoke(p1: Websocket) {
            websocket = p1
            p1.onMessage {
                messages += it
            }
            p1.onClose {
                closed.set(it)
            }
            p1.onError {
                throwable.add(it)
            }
        }
    }

    @Test
    fun `when match, passes a consumer with the matching request`() {
        val consumer = TestConsumer();

        { _: Request -> consumer }.testWebsocket(Request(Method.GET, "/"))

        assertThat(consumer.websocket.upgradeRequest, equalTo(Request(Method.GET, "/")))
    }

    @Test
    fun `sends outbound messages to the websocket`() {
        val consumer = TestConsumer()
        val client = { _: Request -> consumer }.testWebsocket(Request(Method.GET, "/"))

        client.send(message)
        assertThat(consumer.messages, equalTo(listOf(message)))
        client.close(WsStatus.NEVER_CONNECTED)
        assertThat(consumer.closed.get(), equalTo(WsStatus.NEVER_CONNECTED))
    }

    @Test
    fun `sends inbound messages to the client`() {
        val consumer = TestConsumer()
        val client = { _: Request -> consumer }.testWebsocket(Request(Method.GET, "/"))

        val received = mutableListOf<WsMessage>()
        client.onMessage { received += it }

        consumer.websocket.send(message)
        consumer.websocket.send(message)

        assertThat(received, equalTo(listOf(message, message)))
    }
}
