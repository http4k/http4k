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
import org.java_websocket.exceptions.WebsocketNotConnectedException
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import java.time.Duration
import java.time.temporal.ChronoUnit
import java.util.concurrent.LinkedBlockingQueue

class WebsocketClientTest {
    private lateinit var server: Http4kServer

    private val port: Int
        get() = server.port()

    @BeforeEach
    fun before() {
        val ws = websockets(
            "/bin" bind { ws: Websocket ->
                ws.onMessage {
                    val content = it.body.stream.readBytes()
                    ws.send(WsMessage(content.inputStream()))
                    ws.close(NORMAL)
                }
            },

            "/{name}" bind { ws: Websocket ->
                val name = ws.upgradeRequest.path("name")!!
                ws.send(WsMessage(name))
                ws.onMessage {
                    ws.send(it)
                    ws.close(NORMAL)
                }
            },

            "/long-living/{name}" bind { ws: Websocket ->
                val name = ws.upgradeRequest.path("name")!!
                ws.send(WsMessage(name))
                ws.onMessage {
                    ws.send(it)
                    // not sending close
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
    fun `blocking happy path`() {
        val client = WebsocketClient.blocking(Uri.of("ws://localhost:$port/bob"))
        client.send(WsMessage("hello"))
        assertThat(client.received().take(3).toList(), equalTo(listOf(WsMessage("bob"), WsMessage("hello"))))
    }

    @Test
    fun `blocking on a unknown host`() {
        assertThrows<WebsocketNotConnectedException> {
            WebsocketClient.blocking(Uri.of("ws://localhost:1212/"), timeout = Duration.of(10, ChronoUnit.MILLIS))
        }
    }

    @Test
    fun `blocking gets exception on sending after the connection is closed`() {
        val client = WebsocketClient.blocking(Uri.of("ws://localhost:$port/bob"))
        client.send(WsMessage("hello"))
        assertThat(client.received().take(3).toList(), equalTo(listOf(WsMessage("bob"), WsMessage("hello"))))
        assertThrows<WebsocketNotConnectedException> {
            client.send(WsMessage("hi"))
        }
    }

    @Test
    fun `blocking with auto-reconnection (closed by server)`() {
        val client = WebsocketClient.blocking(Uri.of("ws://localhost:$port/bob"), autoReconnection = true)
        client.send(WsMessage("hello"))

        assertThat(client.received().take(3).toList(), equalTo(listOf(WsMessage("bob"), WsMessage("hello"))))

        Thread.sleep(100)

        client.send(WsMessage("hi"))
        assertThat(client.received().take(3).toList(), equalTo(listOf(WsMessage("bob"), WsMessage("hi"))))
    }

    @Test
    fun `blocking with auto-reconnection (closed by client)`() {
        val client = WebsocketClient.blocking(Uri.of("ws://localhost:$port/long-living/bob"), autoReconnection = true)

        client.send(WsMessage("hello"))
        Thread.sleep(1000) // wait until the message comes back

        client.close()

        assertThat(client.received().take(3).toList(), equalTo(listOf(WsMessage("bob"), WsMessage("hello"))))

        client.send(WsMessage("hi"))
        Thread.sleep(1000)
        client.close()

        assertThat(client.received().take(3).toList(), equalTo(listOf(WsMessage("bob"), WsMessage("hi"))))
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

    @Test
    fun `non-blocking - send and receive in binary mode`() {
        val queue = LinkedBlockingQueue<() -> WsMessage?>()
        val received = generateSequence { queue.take()() }

        WebsocketClient.nonBlocking(Uri.of("ws://localhost:$port/bin")) { ws ->
            ws.onMessage { message ->
                queue.add { message }
            }
            ws.send(WsMessage("hello".byteInputStream()))
            ws.onClose {
                queue.add { null }
            }
        }

        assertThat(received.take(4).toList(), equalTo(listOf(WsMessage("hello"))))
    }
}
