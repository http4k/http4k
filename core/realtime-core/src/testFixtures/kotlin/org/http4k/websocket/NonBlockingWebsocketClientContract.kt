package org.http4k.websocket

import com.natpryce.hamkrest.Matcher
import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.equalTo
import com.natpryce.hamkrest.has
import com.natpryce.hamkrest.hasSize
import kotlinx.coroutines.runBlocking
import org.http4k.base64Encode
import org.http4k.core.MemoryBody
import org.http4k.core.StreamBody
import org.http4k.core.Uri
import org.http4k.server.PolyServerConfig
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Timeout
import java.util.concurrent.CountDownLatch
import java.util.concurrent.LinkedBlockingQueue
import java.util.concurrent.TimeUnit

abstract class NonBlockingWebsocketClientContract(
    serverConfig: PolyServerConfig,
    private val websockets: WebsocketFactory
) : BaseWebsocketClientContract(serverConfig) {

    @Test
    @Timeout(10, unit = TimeUnit.SECONDS)
    fun `send and receive in text mode`() = runBlocking {
        val queue = LinkedBlockingQueue<() -> WsMessage?>()
        val received = generateSequence { queue.take()() }

        websockets.nonBlocking(Uri.of("ws://localhost:$port/bob")) { ws ->
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
        }

        assertThat(received.take(4).toList(), equalTo(listOf(WsMessage("bob"), WsMessage("hello"))))
    }

    @Test
    @Timeout(10, unit = TimeUnit.SECONDS)
    fun `send and receive in binary mode - MemoryBody`() = runBlocking {
        val queue = LinkedBlockingQueue<() -> WsMessage?>()
        val received = generateSequence { queue.take()() }

        val content = javaClass.classLoader.getResourceAsStream("org/http4k/websocket/sample_2k.png")!!.readBytes()

        websockets.nonBlocking(Uri.of("ws://localhost:$port/bin")) { ws ->
            ws.onMessage { message ->
                queue.add { message }
            }
            ws.onClose {
                queue.add { null }
            }
            ws.send(WsMessage(MemoryBody(content), WsMessage.Mode.Binary))
        }

        val messages = received.take(4).toList()
        assertThat(messages, hasSize(equalTo(1)))

        val message = messages.first()
        assertThat(message.mode, equalTo(WsMessage.Mode.Binary))
        assertThat(message.body.stream.readBytes().base64Encode(), equalTo(content.base64Encode()))
    }

    @Test
    @Timeout(10, unit = TimeUnit.SECONDS)
    fun `send and receive in binary mode - StreamBody`() = runBlocking {
        val queue = LinkedBlockingQueue<() -> WsMessage?>()
        val received = generateSequence { queue.take()() }

        val content = javaClass.classLoader.getResourceAsStream("org/http4k/websocket/sample_2k.png")!!.readBytes()

        websockets.nonBlocking(Uri.of("ws://localhost:$port/bin")) { ws ->
            ws.onMessage { message ->
                queue.add { message }
            }
            ws.onClose {
                queue.add { null }
            }
            ws.send(WsMessage(StreamBody(content.inputStream()), WsMessage.Mode.Binary))
        }

        val messages = received.take(4).toList()
        assertThat(messages, hasSize(equalTo(1)))

        val message = messages.first()
        assertThat(message.mode, equalTo(WsMessage.Mode.Binary))
        assertThat(message.body.stream.readBytes().base64Encode(), equalTo(content.base64Encode()))
    }

    @Test
    fun `onConnect is called when connected`() = runBlocking {
        val connected = CountDownLatch(1)

        websockets.nonBlocking(Uri.of("ws://localhost:$port/bob")) {
            connected.countDown()
        }

        assertThat(connected, isTrue)
    }

    @Test
    fun `onError is called on connection error`() = runBlocking {
        val error = CountDownLatch(1)

        websockets.nonBlocking(Uri.of("ws://does-not-exist:12345"), onError = { error.countDown() })

        assertThat(error, isTrue)
    }

    @Test
    fun `headers are sent to the server`() = runBlocking {
        val queue = LinkedBlockingQueue<() -> WsMessage?>()
        val received = generateSequence { queue.take()() }

        val ws = websockets.nonBlocking(Uri.of("ws://localhost:$port/headers"), headers = listOf("testOne" to "1", "testTwo" to "2")) {
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

    private val isTrue: Matcher<CountDownLatch> = has("counted down", { it.await(5, TimeUnit.SECONDS) }, equalTo(true))
}
