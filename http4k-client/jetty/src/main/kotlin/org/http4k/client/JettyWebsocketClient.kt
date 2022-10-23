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
import java.util.concurrent.CompletableFuture
import java.util.concurrent.ExecutionException
import java.util.concurrent.LinkedBlockingQueue
import java.util.concurrent.TimeUnit

object JettyWebsocketClient {

    fun blocking(
        uri: Uri,
        headers: Headers = emptyList(),
        timeout: Duration = Duration.of(5, ChronoUnit.SECONDS),
        wsClient: WebSocketClient = WebSocketClient(defaultJettyHttpClient())
    ): WsClient {
        if(!wsClient.isRunning) wsClient.start()

        return BlockingWebsocket(uri, headers, timeout, wsClient).awaitConnected()
    }

    fun nonBlocking(
        uri: Uri,
        headers: Headers = emptyList(),
        timeout: Duration = Duration.ZERO,
        wsClient: WebSocketClient = WebSocketClient(defaultJettyHttpClient()),
        onError: (Throwable) -> Unit = {},
        onConnect: WsConsumer = {}
    ): Websocket {
        if(!wsClient.isRunning) wsClient.start()

        return NonBlockingWebsocket(uri, headers, timeout, wsClient, onError, onConnect)
    }
}

private class BlockingWebsocket(
    uri: Uri,
    headers: Headers,
    timeout: Duration,
    client: WebSocketClient
) : WsClient {
    private val connected = CompletableFuture<WsClient>()

    private val queue = LinkedBlockingQueue<() -> WsMessage?>()

    private val websocket = NonBlockingWebsocket(uri, headers, timeout, client, connected::completeExceptionally) { ws ->
        ws.onMessage { queue += { it } }
        ws.onError { queue += { throw it } }
        ws.onClose { queue += { null } }
        connected.complete(this)
    }

    fun awaitConnected(): WsClient = try {
        connected.get()
    } catch (e: ExecutionException) {
        throw (e.cause ?: e)
    }

    override fun received(): Sequence<WsMessage> = generateSequence { queue.take()() }

    override fun close(status: WsStatus) = websocket.close(status)

    override fun send(message: WsMessage) = websocket.send(message)
}

private class NonBlockingWebsocket(
    uri: Uri,
    headers: Headers,
    timeout: Duration,
    client: WebSocketClient,
    onError: (Throwable) -> Unit,
    private val onConnect: WsConsumer
) : PushPullAdaptingWebSocket(Request(Method.GET, uri).headers(headers)) {

    private val listener = Listener()

    init {
        onError(onError)
        client.connect(listener, URI.create(uri.toString()), clientUpgradeRequest(headers, timeout))
            .whenComplete { _, error -> triggerError(error) }
    }

    override fun send(message: WsMessage) = with(listener) {
        if (isNotConnected) {
            throw WebSocketException("Connection to ${upgradeRequest.uri} is closed.")
        }
        when (message.body) {
            is StreamBody -> remote.sendBytes(message.body.payload)
            else -> remote.sendString(message.body.toString())
        }
    }

    override fun close(status: WsStatus) = with(listener) {
        if (isConnected) {
            session.close(status.code, status.description)
        }
    }

    inner class Listener : WebSocketAdapter() {
        override fun onWebSocketConnect(session: Session) {
            super.onWebSocketConnect(session)
            onConnect(this@NonBlockingWebsocket)
        }

        override fun onWebSocketClose(statusCode: Int, reason: String?) {
            triggerClose(WsStatus(statusCode, reason.orEmpty()))
        }

        override fun onWebSocketText(message: String) {
            triggerMessage(WsMessage(message))
        }

        override fun onWebSocketBinary(payload: ByteArray, offset: Int, len: Int) {
            triggerMessage(WsMessage(payload.inputStream(offset, len)))
        }

        override fun onWebSocketError(cause: Throwable) {
            triggerError(cause)
        }
    }
}

private fun clientUpgradeRequest(headers: Headers, timeout: Duration) = ClientUpgradeRequest().apply {
    setHeaders(headers.toParametersMap())
    setTimeout(timeout.toMillis(), TimeUnit.MILLISECONDS)
}
