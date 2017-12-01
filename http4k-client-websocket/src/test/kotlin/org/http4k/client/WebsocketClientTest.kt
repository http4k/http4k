package org.http4k.client

import com.natpryce.hamkrest.equalTo
import com.natpryce.hamkrest.should.shouldMatch
import org.http4k.core.Status.Companion.OK
import org.http4k.core.Uri
import org.http4k.routing.bind
import org.http4k.routing.path
import org.http4k.routing.websockets
import org.http4k.server.Http4kServer
import org.http4k.server.Jetty
import org.http4k.server.asServer
import org.http4k.util.RetryRule
import org.http4k.websocket.Websocket
import org.http4k.websocket.WsMessage
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import java.util.Random
import java.util.concurrent.LinkedBlockingQueue

class WebsocketClientTest {
    private lateinit var server: Http4kServer

    @Rule
    @JvmField
    var retryRule = RetryRule(5)

    private val port = Random().nextInt(1000) + 8000

    @Before
    fun before() {
        val ws = websockets(
            "/{name}" bind { ws: Websocket ->
                val name = ws.upgradeRequest.path("name")!!
                ws.send(WsMessage(name))
                ws.onMessage {
                    ws.send(it)
                    ws.close(OK)
                }
            }
        )
        server = ws.asServer(Jetty(port)).start()
    }

    @After
    fun after() {
        server.stop()
    }

    @Test
    fun `blocking`() {
        val client = WebsocketClient.blocking(Uri.of("ws://localhost:$port/bob"))
        client.send(WsMessage("hello"))
        client.received.take(3).toList() shouldMatch equalTo(listOf(WsMessage("bob"), WsMessage("hello")))
    }

    @Test
    fun `non-blocking`() {
        val queue = LinkedBlockingQueue<() -> WsMessage?>()
        val received = generateSequence { queue.take()() }

        val client = WebsocketClient.nonBlocking(Uri.of("ws://localhost:$port/bob"))
        var sent = false
        client.onMessage {
            if(!sent) {
                sent = true
                client.send(WsMessage("hello"))
            }
            queue.add { it }
        }
        client.onClose {
            queue.add { null }
        }

        received.take(4).toList() shouldMatch equalTo(listOf(WsMessage("bob"), WsMessage("hello")))
    }
}