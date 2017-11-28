package org.http4k.websocket

import com.natpryce.hamkrest.absent
import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.equalTo
import com.natpryce.hamkrest.should.shouldMatch
import org.http4k.core.Method
import org.http4k.core.Request
import org.http4k.core.Status
import org.junit.Test
import java.util.concurrent.atomic.AtomicReference

class WsClientTest {

    private val message = WsMessage("hello")
    private val error = RuntimeException("foo") as Throwable

    private class TestConsumer : WsConsumer {
        lateinit var websocket: WebSocket
        val messages = mutableListOf<WsMessage>()
        val throwable = mutableListOf<Throwable>()
        val closed = AtomicReference<Status>()

        override fun invoke(p1: WebSocket) {
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

        { _: Request -> consumer }.asClient(Request(Method.GET, "/"))!!

        consumer.websocket.upgradeRequest shouldMatch equalTo(Request(Method.GET, "/"))
    }

    @Test
    fun `sends outbound messages to the websocket`() {
        val consumer = TestConsumer()
        val client: WsClient = { _: Request -> consumer }.asClient(Request(Method.GET, "/"))!!

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
            { ws: WebSocket ->
                ws.send(message)
                ws.close(Status.OK)
            }
        }.asClient(Request(Method.GET, "/"))!!

        client.received.toList() shouldMatch equalTo(listOf(message))
    }

    @Test
    fun `throws for no match`() {
        assertThat(
            object : WsHandler {
                override fun invoke(request: Request): WsConsumer? = null
            }.asClient(Request(Method.GET, "/"))
            , absent())
    }
}