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
    fun `when match, passes a consumer with the matching request and can pass through all messages to the websocket`() {
        val consumer = TestConsumer()
        val client: WsClient = object : WsHandler {
            override fun invoke(request: Request): WsConsumer? = consumer
        }.asClient(Request(Method.GET, "/"))!!

        consumer.websocket.upgradeRequest shouldMatch equalTo(Request(Method.GET, "/"))

        client.send(WsMessage("hello"))
        consumer.messages shouldMatch equalTo(listOf(WsMessage("hello")))
        val error = RuntimeException("foo") as Throwable
        client.error(error)
        consumer.throwable shouldMatch equalTo(listOf(error))
        client.close(Status.OK)
        consumer.closed.get() shouldMatch equalTo(Status.OK)
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