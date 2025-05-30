package org.http4k.websocket

import com.natpryce.hamkrest.Matcher
import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.equalTo
import com.natpryce.hamkrest.hasSize
import com.natpryce.hamkrest.throws
import org.http4k.base64Encode
import org.http4k.core.MemoryBody
import org.http4k.core.StreamBody
import org.http4k.core.Uri
import org.http4k.server.PolyServerConfig
import org.junit.jupiter.api.Test
import java.time.Duration

abstract class BlockingWebsocketClientContract(
    serverConfig: PolyServerConfig,
    private val websocketFactory: (timeout: Duration) -> WebsocketFactory,
    private val connectionErrorTimeout: Duration = Duration.ofSeconds(1)
) : BaseWebsocketClientContract(serverConfig) {

    private val websockets = websocketFactory(Duration.ofSeconds(5))
    abstract fun <T: Throwable> connectErrorMatcher(): Matcher<T>
    abstract fun <T: Throwable> connectionClosedErrorMatcher(): Matcher<T>

    @Test
    fun `send and receive in text mode`() {
        val ws = websockets.blocking(Uri.of("ws://localhost:$port/bob"))
        ws.send(WsMessage("hello"))

        val messages = ws.received().take(2).toList()

        assertThat(messages, equalTo(listOf(WsMessage("bob"), WsMessage("hello"))))
    }

    @Test
    fun `send and receive in binary mode - memoryBody`() {
        val ws = websockets.blocking(Uri.of("ws://localhost:$port/bin"))

        val content = javaClass.classLoader.getResourceAsStream("org/http4k/websocket/sample_2k.png")!!.readBytes()

        ws.send(WsMessage(MemoryBody(content), WsMessage.Mode.Binary))

        val messages = ws.received().take(4).toList()
        assertThat(messages, hasSize(equalTo(1)))

        val message = messages.first()
        assertThat(message.mode, equalTo(WsMessage.Mode.Binary))
        assertThat(message.body.stream.readBytes().base64Encode(), equalTo(content.base64Encode()))
    }

    @Test
    fun `send and receive in binary mode - StreamBody`() {
        val ws = websockets.blocking(Uri.of("ws://localhost:$port/bin"))

        val content = javaClass.classLoader.getResourceAsStream("org/http4k/websocket/sample_2k.png")!!.readBytes()

        ws.send(WsMessage(StreamBody(content.inputStream()), WsMessage.Mode.Binary))

        val messages = ws.received().take(4).toList()
        assertThat(messages, hasSize(equalTo(1)))

        val message = messages.first()
        assertThat(message.mode, equalTo(WsMessage.Mode.Binary))
        assertThat(message.body.stream.readBytes().base64Encode(), equalTo(content.base64Encode()))
    }

    @Test
    open fun `exception is thrown on connection error`() {
        assertThat({ websocketFactory(connectionErrorTimeout).blocking(Uri.of("ws://does-not-exist:12345")) },
            throws(connectErrorMatcher()))
    }

    @Test
    fun `exception is thrown on sending after connection is closed`() {
        val ws = websockets.blocking(Uri.of("ws://localhost:$port/bob"))
        ws.send(WsMessage("hello"))

        val messages = ws.received().take(3).toList()

        assertThat(messages, equalTo(listOf(WsMessage("bob"), WsMessage("hello"))))
        assertThat({ ws.send(WsMessage("hi")) }, throws(connectionClosedErrorMatcher()))
    }

    @Test
    fun `headers are sent to the server`() {
        val ws = websockets.blocking(Uri.of("ws://localhost:$port/headers"), headers = listOf("testOne" to "1", "testTwo" to "2"))
        ws.send(WsMessage(""))

        val messages = ws.received().take(4).toList()

        assertThat(messages, equalTo(listOf(WsMessage("testOne=1"), WsMessage("testTwo=2"))))
    }
}
