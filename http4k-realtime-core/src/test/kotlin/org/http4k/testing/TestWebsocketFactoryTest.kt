package org.http4k.testing

import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.equalTo
import org.http4k.routing.path
import org.http4k.routing.websockets
import org.http4k.routing.ws.bind
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
        "/hello/{name}" bind { req ->
            val name = req.path("name")!!
            WsResponse { ws ->
                ws.send(WsMessage(name))
                ws.close()
            }
        }
    )

    private val factory = TestWebsocketFactory(wsHandler)

    @Test
    fun `blocking client`() {
        assertThat(
            factory.blocking("hello/jim").received().toList(),
            equalTo(listOf(WsMessage("jim")))
        )
    }

    @Test
    @Timeout(2, unit = TimeUnit.SECONDS)
    fun `non-blocking client`() {
        val latch = CountDownLatch(1)
        val ws = factory.nonBlocking("hello/jim")
        ws.onMessage {
            assertThat(it, equalTo(WsMessage("jim")))
            latch.countDown()
        }

        latch.await()
    }
}
