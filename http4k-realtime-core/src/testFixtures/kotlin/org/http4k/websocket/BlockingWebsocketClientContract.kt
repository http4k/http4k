package org.http4k.websocket

import com.natpryce.hamkrest.Matcher
import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.equalTo
import com.natpryce.hamkrest.throws
import org.http4k.core.Headers
import org.http4k.core.StreamBody
import org.http4k.core.Uri
import org.http4k.server.PolyServerConfig
import org.junit.jupiter.api.Test
import java.time.Duration

abstract class BlockingWebsocketClientContract(
    serverConfig: PolyServerConfig,
    private val websocketFactory: (Uri, Headers, Duration) -> WsClient,
    private val connectionErrorTimeout: Duration = Duration.ofSeconds(1)
) : BaseWebsocketClientContract(serverConfig) {

    abstract fun <T: Throwable> connectErrorMatcher(): Matcher<T>
    abstract fun <T: Throwable> connectionClosedErrorMatcher(): Matcher<T>

    @Test
    fun `send and receive in text mode`() {
        val ws = websocket(Uri.of("ws://localhost:$port/bob"))
        ws.send(WsMessage("hello"))

        val messages = ws.received().take(4).toList()

        assertThat(messages, equalTo(listOf(WsMessage("bob"), WsMessage("hello"))))
    }

    @Test
    fun `send and receive in binary mode`() {
        val ws = websocket(Uri.of("ws://localhost:$port/bin"))
        ws.send(WsMessage("hello".byteInputStream()))

        val messages = ws.received().take(4).toList()

        assertThat(messages.all { it.body is StreamBody }, equalTo(true))
        assertThat(messages, equalTo(listOf(WsMessage("hello"))))
    }


    @Test
    fun `exception is thrown on connection error`() {
        assertThat({ websocket(Uri.of("ws://does-not-exist:12345"), timeout = connectionErrorTimeout) },
            throws(connectErrorMatcher()))
    }

    @Test
    fun `exception is thrown on sending after connection is closed`() {
        val ws = websocket(Uri.of("ws://localhost:$port/bob"))
        ws.send(WsMessage("hello"))

        val messages = ws.received().take(3).toList()

        assertThat(messages, equalTo(listOf(WsMessage("bob"), WsMessage("hello"))))
        assertThat({ ws.send(WsMessage("hi")) }, throws(connectionClosedErrorMatcher()))
    }

    @Test
    fun `headers are sent to the server`() {
        val ws = websocket(Uri.of("ws://localhost:$port/headers"), headers = listOf("testOne" to "1", "testTwo" to "2"))
        ws.send(WsMessage(""))

        val messages = ws.received().take(4).toList()

        assertThat(messages, equalTo(listOf(WsMessage("testOne=1"), WsMessage("testTwo=2"))))
    }

    private fun websocket(uri: Uri, headers: Headers = emptyList(), timeout: Duration = Duration.ofSeconds(3)): WsClient =
        websocketFactory(uri, headers, timeout)
}
