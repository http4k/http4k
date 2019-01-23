package org.http4k.client

import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.equalTo
import org.http4k.core.Uri
import org.http4k.routing.bind
import org.http4k.routing.path
import org.http4k.routing.websockets
import org.http4k.server.Http4kServer
import org.http4k.server.Jetty
import org.http4k.server.asServer
import org.http4k.websocket.Websocket
import org.http4k.websocket.WsMessage
import org.http4k.websocket.WsStatus.Companion.NORMAL
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.util.concurrent.LinkedBlockingQueue

class WebsocketClientTest {
    private lateinit var server: Http4kServer

    private val port: Int
        get() = server.port()

    @BeforeEach
    fun before() {
        val ws = websockets(
            "/{name}" bind { ws: Websocket ->
                val name = ws.upgradeRequest.path("name")!!
                ws.send(WsMessage(name))
                ws.onMessage {
                    ws.send(it)
                    ws.close(NORMAL)
                }
            }
        )
        server = ws.asServer(Jetty(0)).start()
    }

    @AfterEach
    fun after() {
        server.stop()
    }

    @Test
    fun `blocking`() {
        val client = WebsocketClient.blocking(Uri.of("ws://localhost:$port/bob"))
        client.send(WsMessage("hello"))
        assertThat(client.received().take(3).toList(), equalTo(listOf(WsMessage("bob"), WsMessage("hello"))))
    }

    @Test
    fun `non-blocking`() {
        val queue = LinkedBlockingQueue<() -> WsMessage?>()
        val received = generateSequence { queue.take()() }
        var connected = false

        val websocket = WebsocketClient.nonBlocking(Uri.of("ws://localhost:$port/bob")) {
            connected = true
        }
        var sent = false
        websocket.onMessage {
            if (!sent) {
                sent = true
                websocket.send(WsMessage("hello"))
            }
            queue.add { it }
        }
        websocket.onClose {
            queue.add { null }
        }

        assertThat(received.take(4).toList(), equalTo(listOf(WsMessage("bob"), WsMessage("hello"))))
        assertThat(connected, equalTo(true))
    }
}