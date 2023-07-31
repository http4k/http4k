package org.http4k.websocket

import com.natpryce.hamkrest.Matcher
import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.equalTo
import com.natpryce.hamkrest.has
import org.http4k.core.Headers
import org.http4k.core.StreamBody
import org.http4k.core.Uri
import org.http4k.hamkrest.hasUri
import org.http4k.server.PolyServerConfig
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Timeout
import java.util.concurrent.CountDownLatch
import java.util.concurrent.LinkedBlockingQueue
import java.util.concurrent.TimeUnit

abstract class NonBlockingWebsocketClientContract(
    serverConfig: PolyServerConfig,
    private val websocketFactory: (Uri, Headers, (Throwable) -> Unit, WsConsumer) -> Websocket
) : BaseWebsocketClientContract(serverConfig) {

    @Test
    @Timeout(10, unit = TimeUnit.SECONDS)
    fun `send and receive in text mode`() {
        val queue = LinkedBlockingQueue<() -> WsMessage?>()
        val received = generateSequence { queue.take()() }

        val ws = websocket(Uri.of("ws://localhost:$port/bob"))
        var sent = false
        ws.onMessage {
            if (!sent) {
                sent = true
                ws.send(WsMessage("hello"))
            }
            queue.add { it }
        }
        ws.onClose {
            queue.add { null }
        }

        assertThat(received.take(4).toList(), equalTo(listOf(WsMessage("bob"), WsMessage("hello"))))
    }

    @Test
    @Timeout(10, unit = TimeUnit.SECONDS)
    fun `send and receive in binary mode`() {
        val queue = LinkedBlockingQueue<() -> WsMessage?>()
        val received = generateSequence { queue.take()() }

        websocket(Uri.of("ws://localhost:$port/bin")) { ws ->
            ws.onMessage { message ->
                queue.add { message }
            }
            ws.onClose {
                queue.add { null }
            }
            ws.send(WsMessage("hello".byteInputStream()))
        }

        val messages = received.take(4).toList()
        assertThat(messages.all { it.body is StreamBody }, equalTo(true))
        assertThat(messages, equalTo(listOf(WsMessage("hello"))))
    }

    @Test
    fun `onConnect is called when connected`() {
        val connected = CountDownLatch(1)

        websocket(Uri.of("ws://localhost:$port/bob")) {
            connected.countDown()
        }

        assertThat(connected, isTrue)
    }

    @Test
    fun `onError is called on connection error`() {
        val error = CountDownLatch(1)

        websocket(Uri.of("ws://does-not-exist:12345"), onError = { error.countDown() })

        assertThat(error, isTrue)
    }

    @Test
    fun `headers are sent to the server`() {
        val queue = LinkedBlockingQueue<() -> WsMessage?>()
        val received = generateSequence { queue.take()() }

        val ws = websocket(Uri.of("ws://localhost:$port/headers"), headers = listOf("testOne" to "1", "testTwo" to "2")) {
            it.send(WsMessage(""))
        }
        ws.onMessage {
            queue.add { it }
        }
        ws.onClose {
            queue.add { null }
        }

        assertThat(received.take(4).toList(), equalTo(listOf(WsMessage("testOne=1"), WsMessage("testTwo=2"))))
    }

    private fun websocket(uri: Uri, headers: Headers = emptyList(), onError: (Throwable) -> Unit = {},
                          onConnect: WsConsumer = {}): Websocket =
        websocketFactory(uri, headers, onError, onConnect)

    private val isTrue: Matcher<CountDownLatch> = has("counted down", { it.await(5, TimeUnit.SECONDS) }, equalTo(true))
}
