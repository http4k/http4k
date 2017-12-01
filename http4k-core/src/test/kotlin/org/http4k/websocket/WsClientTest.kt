package org.http4k.websocket

import com.natpryce.hamkrest.absent
import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.equalTo
import com.natpryce.hamkrest.should.shouldMatch
import com.natpryce.hamkrest.throws
import org.http4k.core.Method
import org.http4k.core.Request
import org.http4k.core.Status
import org.http4k.testing.ClosedWebsocket
import org.http4k.testing.testWsClient
import org.junit.Test
import java.util.concurrent.atomic.AtomicReference

class WsClientTest {

    private val message = WsMessage("hello")
    private val error = RuntimeException("foo") as Throwable

    private class TestConsumer : WsConsumer {
        lateinit var websocket: Websocket
        val messages = mutableListOf<WsMessage>()
        val throwable = mutableListOf<Throwable>()
        val closed = AtomicReference<Status>()

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

        { _: Request -> consumer }.testWsClient(Request(Method.GET, "/"))!!

        consumer.websocket.upgradeRequest shouldMatch equalTo(Request(Method.GET, "/"))
    }

    @Test
    fun `sends outbound messages to the websocket`() {
        val consumer = TestConsumer()
        val client = { _: Request -> consumer }.testWsClient(Request(Method.GET, "/"))!!

        client.send(message)
        consumer.messages shouldMatch equalTo(listOf(message))
        client.error(error)
        consumer.throwable shouldMatch equalTo(listOf(error))
        client.close(Status.OK)
        consumer.closed.get() shouldMatch equalTo(Status.OK)
    }

    @Test
    fun `sends inbound messages to the client`() {
        val client = { _: Request ->
            { ws: Websocket ->
                ws.send(message)
                ws.close(Status.OK)
            }
        }.testWsClient(Request(Method.GET, "/"))!!

        val received = client.received()
        received.take(1).first() shouldMatch equalTo(message)
    }

    @Test
    fun `closed websocket throws when read attempted`() {
        val client = { _: Request ->
            { ws: Websocket ->
                ws.close(Status.OK)
            }
        }.testWsClient(Request(Method.GET, "/"))!!

        assertThat({ client.received().take(2).toList() }, throws<ClosedWebsocket>(equalTo(ClosedWebsocket(Status.OK))))
    }

    @Test
    fun `throws for no match`() {
        assertThat(
            object : WsHandler {
                override fun invoke(request: Request): WsConsumer? = null
            }.testWsClient(Request(Method.GET, "/"))
            , absent())
    }
}