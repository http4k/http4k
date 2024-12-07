package org.http4k.testing

import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.equalTo
import org.http4k.routing.websocket.bind
import org.http4k.routing.websockets
import org.http4k.websocket.WsMessage
import org.http4k.websocket.WsResponse
import org.http4k.websocket.blocking
import org.http4k.websocket.nonBlocking
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Timeout
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

class TestWebsocketFactoryTest {

    private val wsHandler = websockets(
        "/echo" bind {
            WsResponse { ws ->
                ws.onMessage {
                    ws.send(it)
                    ws.close()
                }
            }
        }
    )

    private val factory = TestWebsocketFactory(wsHandler)

    @Test
    fun `blocking client`() {
        val ws = factory.blocking("/echo")
        ws.send(WsMessage("hello"))
        assertThat(
            ws.received().toList(),
            equalTo(listOf(WsMessage("hello")))
        )
    }

    @Test
    @Timeout(2, unit = TimeUnit.SECONDS)
    fun `non-blocking client`() {
        val latch = CountDownLatch(1)
        val ws = factory.nonBlocking("/echo")
        ws.onMessage {
            assertThat(it, equalTo(WsMessage("hello")))
            latch.countDown()
        }
        ws.send(WsMessage("hello"))

        latch.await()
    }
}
