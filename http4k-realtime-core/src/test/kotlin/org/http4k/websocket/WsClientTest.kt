package org.http4k.websocket

import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.equalTo
import com.natpryce.hamkrest.isEmpty
import com.natpryce.hamkrest.throws
import org.http4k.core.Method.GET
import org.http4k.core.Request
import org.http4k.testing.ClosedWebsocket
import org.http4k.testing.testWsClient
import org.http4k.websocket.WsStatus.Companion.NEVER_CONNECTED
import org.http4k.websocket.WsStatus.Companion.NORMAL
import org.junit.jupiter.api.Test
import java.util.concurrent.atomic.AtomicReference

class WsClientTest {

    private val message = WsMessage("hello")
    private val error = RuntimeException("foo") as Throwable

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

        { _: Request -> consumer }.testWsClient(Request(GET, "/"))

        assertThat(consumer.websocket.upgradeRequest, equalTo(Request(GET, "/")))
    }

    @Test
    fun `sends outbound messages to the websocket`() {
        val consumer = TestConsumer()
        val client = { _: Request -> consumer }.testWsClient(Request(GET, "/"))

        client.send(message)
        assertThat(consumer.messages, equalTo(listOf(message)))
        client.error(error)
        assertThat(consumer.throwable, equalTo(listOf(error)))
        client.close(NEVER_CONNECTED)
        assertThat(consumer.closed.get(), equalTo(NEVER_CONNECTED))
    }

    @Test
    fun `sends inbound messages to the client`() {
        val client = { _: Request ->
            { ws: Websocket ->
                ws.send(message)
                ws.send(message)
                ws.close(NEVER_CONNECTED)
            }
        }.testWsClient(Request(GET, "/"))

        val received = client.received()
        assertThat(received.take(2).toList(), equalTo(listOf(message, message)))
    }

    @Test
    fun `closed websocket throws when read attempted`() {
        val client = { _: Request ->
            { ws: Websocket ->
                ws.close(NEVER_CONNECTED)
            }
        }.testWsClient(Request(GET, "/"))

        assertThat({ client.received().take(2).toList() }, throws(equalTo(ClosedWebsocket(NEVER_CONNECTED))))
    }

    @Test
    fun `throws for no match`() {
        val actual = object : WsHandler {
            override fun invoke(request: Request): WsConsumer = { it.close(NEVER_CONNECTED) }
        }.testWsClient(Request(GET, "/"))

        assertThat({ actual.received().toList() }, throws<ClosedWebsocket>())
    }

    @Test
    fun `when no messages`() {
        val client = { _: Request ->
            { ws: Websocket ->
                ws.close(NORMAL)
            }
        }.testWsClient(Request(GET, "/"))

        assertThat(client.received().none(), equalTo(true))
        assertThat(client.received().toList(), isEmpty) // verify NoSuchElement not thrown during iteration
    }
}
