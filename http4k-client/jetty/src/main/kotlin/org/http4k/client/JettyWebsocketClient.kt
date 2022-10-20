package org.http4k.client

import org.eclipse.jetty.websocket.api.Session
import org.eclipse.jetty.websocket.api.WebSocketAdapter
import org.eclipse.jetty.websocket.api.exceptions.WebSocketException
import org.eclipse.jetty.websocket.client.ClientUpgradeRequest
import org.eclipse.jetty.websocket.client.WebSocketClient
import org.http4k.client.PreCannedJettyHttpClients.defaultJettyHttpClient
import org.http4k.core.Headers
import org.http4k.core.Method
import org.http4k.core.Request
import org.http4k.core.StreamBody
import org.http4k.core.Uri
import org.http4k.core.toParametersMap
import org.http4k.websocket.PushPullAdaptingWebSocket
import org.http4k.websocket.Websocket
import org.http4k.websocket.WsClient
import org.http4k.websocket.WsConsumer
import org.http4k.websocket.WsMessage
import org.http4k.websocket.WsStatus
import java.net.URI
import java.time.Duration
import java.time.temporal.ChronoUnit
import java.util.concurrent.BlockingQueue
import java.util.concurrent.ExecutionException
import java.util.concurrent.LinkedBlockingQueue
import java.util.concurrent.TimeUnit

object JettyWebsocketClient {

    fun blockingWebsocket(
        uri: Uri,
        headers: Headers = emptyList(),
        timeout: Duration = Duration.of(5, ChronoUnit.SECONDS),
        wsClient: WebSocketClient = WebSocketClient(defaultJettyHttpClient())
    ): WsClient {
        if(!wsClient.isRunning) wsClient.start()

        val queue = LinkedBlockingQueue<() -> WsMessage?>()

        val listener = BlockingQueueWsClient(queue)
        return runCatching {
            wsClient.connect(listener, URI.create(uri.toString()), clientUpgradeRequest(headers, timeout))
            .get(timeout.toMillis(), TimeUnit.MILLISECONDS)
        }.exceptionOrNull()?.let { connectError ->
            throw when (connectError) {
                is ExecutionException -> connectError.cause ?: connectError
                else -> connectError
            }
        } ?: listener
    }

    fun nonBlockingWebsocket(
        uri: Uri,
        headers: Headers = emptyList(),
        timeout: Duration = Duration.ZERO,
        wsClient: WebSocketClient = WebSocketClient(defaultJettyHttpClient()),
        onError: (Throwable) -> Unit = {},
        onConnect: WsConsumer = {}
    ): Websocket {
        if(!wsClient.isRunning) wsClient.start()

        val listener = NonBlockingWebsocket(uri, onError, onConnect)
        wsClient.connect(listener, URI.create(uri.toString()), clientUpgradeRequest(headers, timeout))
            .whenComplete { _, error -> onError(error) }
        return listener
    }

    private fun clientUpgradeRequest(headers: Headers, timeout: Duration) = ClientUpgradeRequest().apply {
        setHeaders(headers.toParametersMap())
        setTimeout(timeout.toMillis(), TimeUnit.MILLISECONDS)
    }
}

private class BlockingQueueWsClient(
    private val queue: BlockingQueue<() -> WsMessage?>
) : WsClient, WebSocketAdapter() {

    override fun onWebSocketClose(statusCode: Int, reason: String?) {
        queue += { null }
    }

    override fun onWebSocketText(message: String) {
        queue += { WsMessage(message) }
    }

    override fun onWebSocketBinary(payload: ByteArray, offset: Int, len: Int) {
        queue += { WsMessage(payload.inputStream(offset, len)) }
    }

    override fun onWebSocketError(cause: Throwable) {
        throw cause
    }

    override fun received(): Sequence<WsMessage> = generateSequence { queue.take()() }

    override fun close(status: WsStatus) = session.close(status.code, status.description)

    override fun send(message: WsMessage) {
        if (isNotConnected) {
            throw WebSocketException("not connected")
        }
        when (message.body) {
            is StreamBody -> remote.sendBytes(message.body.payload)
            else -> remote.sendString(message.body.toString())
        }
    }
}

private class NonBlockingWebsocket(
    uri: Uri,
    onError: (Throwable) -> Unit,
    private val onConnect: WsConsumer
) : Websocket, WebSocketAdapter() {

    private val socket: PushPullAdaptingWebSocket = object : PushPullAdaptingWebSocket(Request(Method.GET, uri)) {
        override fun send(message: WsMessage) {
            if (isNotConnected) {
                throw WebSocketException("not connected")
            }
            when (message.body) {
                is StreamBody -> remote.sendBytes(message.body.payload)
                else -> remote.sendString(message.body.toString())
            }
        }

        override fun close(status: WsStatus) {
            if (isConnected) {
                session.close(status.code, status.description)
            }
        }
    }.apply { onError(onError) }

    override fun onWebSocketConnect(session: Session) {
        super.onWebSocketConnect(session)
        onConnect(socket)
    }

    override fun onWebSocketClose(statusCode: Int, reason: String?) {
        socket.triggerClose(WsStatus(statusCode, reason.orEmpty()))
    }

    override fun onWebSocketText(message: String) {
        socket.triggerMessage(WsMessage(message))
    }

    override fun onWebSocketBinary(payload: ByteArray, offset: Int, len: Int) {
        socket.triggerMessage(WsMessage(payload.inputStream(offset, len)))
    }

    override fun onWebSocketError(cause: Throwable) {
        socket.triggerError(cause)
    }

    override val upgradeRequest: Request = socket.upgradeRequest
    override fun send(message: WsMessage) = socket.send(message)
    override fun close(status: WsStatus) = socket.close(status)
    override fun onError(fn: (Throwable) -> Unit) = socket.onError(fn)
    override fun onClose(fn: (WsStatus) -> Unit) = socket.onClose(fn)
    override fun onMessage(fn: (WsMessage) -> Unit) = socket.onMessage(fn)
}
