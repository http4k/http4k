package org.http4k.client

import io.helidon.common.buffers.BufferData
import io.helidon.webclient.websocket.WsClient
import io.helidon.websocket.WsListener
import io.helidon.websocket.WsSession
import org.http4k.core.Headers
import org.http4k.core.Uri
import org.http4k.websocket.PushPullAdaptingWebSocket
import org.http4k.websocket.Websocket
import org.http4k.websocket.WebsocketFactory
import org.http4k.websocket.WsClient as Http4kWsClient
import org.http4k.websocket.WsConsumer
import org.http4k.websocket.WsMessage
import org.http4k.websocket.WsMessage.Mode.Binary
import org.http4k.websocket.WsMessage.Mode.Text
import org.http4k.websocket.WsStatus
import org.http4k.websocket.toWsClient
import java.time.Duration
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

class HelidonWebsocketClient(
    private val timeout: Duration = Duration.ofSeconds(5)
): WebsocketFactory {

    override fun nonBlocking(
        uri: Uri,
        headers: Headers,
        onError: (Throwable) -> Unit,
        onConnect: WsConsumer
    ): Websocket {
        val (listener, ws) = createWebsocket(onConnect)

        try {
            val client = WsClient.create {
                for ((key, value) in headers) {
                    it.addHeader(key.toHelidonHeaderName(), value)
                }
            }
            client.connect(uri.toString(), listener)
        } catch (e: Throwable) {
            onError(e)
        }

        return ws
    }

    override fun blocking(uri: Uri, headers: Headers): Http4kWsClient {
        val latch = CountDownLatch(1)

        val ws = nonBlocking(
            uri, headers,
            onError = { throw it },
            onConnect = { latch.countDown() }
        )

        if (!latch.await(timeout.toMillis(), TimeUnit.MILLISECONDS)) {
            error("Websocket failed to connect to $uri in $timeout")
        }

        return ws.toWsClient()
    }
}


private fun createWebsocket(onOpen: WsConsumer): Pair<WsListener, PushPullAdaptingWebSocket> {
    lateinit var connection: WsSession

    val ws = object: PushPullAdaptingWebSocket() {
        override fun send(message: WsMessage) {
            when(message.mode) {
                Text -> connection.send(message.bodyString(), true)
                Binary -> connection.send(BufferData.create(message.body.payload.array()), true)
            }
        }

        override fun close(status: WsStatus) {
            connection.close(status.code, status.description)
        }
    }

    val listener = object: WsListener {
        override fun onOpen(session: WsSession) {
            connection = session
            onOpen(ws)
        }

        override fun onMessage(session: WsSession, buffer: BufferData, last: Boolean) {
            ws.triggerMessage(WsMessage(buffer.readBytes()))
        }

        override fun onMessage(session: WsSession, text: String, last: Boolean) {
            System.err.println("SENDING $text")
            ws.triggerMessage(WsMessage(text))
        }

        override fun onClose(session: WsSession, status: Int, reason: String) {
            ws.triggerClose(WsStatus(status, reason))
        }

        override fun onError(session: WsSession, t: Throwable) {
            ws.triggerError(t)
        }
    }

    return listener to ws
}
