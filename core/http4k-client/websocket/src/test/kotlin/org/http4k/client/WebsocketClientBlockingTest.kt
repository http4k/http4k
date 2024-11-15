package org.http4k.client

import com.natpryce.hamkrest.Matcher
import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.equalTo
import com.natpryce.hamkrest.isA
import org.http4k.core.Uri
import org.http4k.server.Jetty
import org.http4k.websocket.BlockingWebsocketClientContract
import org.http4k.websocket.WsMessage
import org.java_websocket.exceptions.WebsocketNotConnectedException
import org.junit.jupiter.api.Test
import java.time.Duration

class WebsocketClientBlockingTest : BlockingWebsocketClientContract(
    serverConfig = Jetty(0),
    websocketFactory = { uri, headers, timeout ->
        WebsocketClient.blocking(uri, headers, timeout)
    },
    connectionErrorTimeout = Duration.ofMillis(10)
) {
    override fun <T : Throwable> connectErrorMatcher(): Matcher<T> = isA<WebsocketNotConnectedException>()

    override fun <T : Throwable> connectionClosedErrorMatcher(): Matcher<T> = isA<WebsocketNotConnectedException>()

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
}
