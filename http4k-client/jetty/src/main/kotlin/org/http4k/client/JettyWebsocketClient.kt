package org.http4k.client

import org.eclipse.jetty.util.BufferUtil
import org.eclipse.jetty.websocket.api.Callback
import org.eclipse.jetty.websocket.api.Callback.Completable
import org.eclipse.jetty.websocket.api.Session
import org.eclipse.jetty.websocket.api.exceptions.WebSocketException
import org.eclipse.jetty.websocket.client.ClientUpgradeRequest
import org.eclipse.jetty.websocket.client.WebSocketClient
import org.http4k.client.PreCannedJettyHttpClients.defaultJettyHttpClient
import org.http4k.core.Headers
import org.http4k.core.Method.GET
import org.http4k.core.Request
import org.http4k.core.Uri
import org.http4k.core.toParametersMap
import org.http4k.websocket.PushPullAdaptingWebSocket
import org.http4k.websocket.Websocket
import org.http4k.websocket.WebsocketFactory
import org.http4k.websocket.WsClient
import org.http4k.websocket.WsConsumer
import org.http4k.websocket.WsMessage
import org.http4k.websocket.WsStatus
import java.net.URI
import java.nio.ByteBuffer
import java.time.Duration
import java.time.temporal.ChronoUnit
import java.util.concurrent.CompletableFuture
import java.util.concurrent.ExecutionException
import java.util.concurrent.LinkedBlockingQueue
import java.util.concurrent.TimeUnit

object JettyWebsocketClient {

    operator fun invoke(
        timeout: Duration =  Duration.of(5, ChronoUnit.SECONDS),
        wsClient: WebSocketClient = WebSocketClient(defaultJettyHttpClient())
    ) = object: WebsocketFactory {
        override fun nonBlocking(uri: Uri, headers: Headers, onError: (Throwable) -> Unit, onConnect: WsConsumer): Websocket {
            if (!wsClient.isRunning) wsClient.start()

            return JettyNonBlockingWebsocket(uri, headers, timeout, wsClient, onError, onConnect)
        }

        override fun blocking(uri: Uri, headers: Headers): WsClient {
            if (!wsClient.isRunning) wsClient.start()

            return JettyBlockingWebsocket(uri, headers, timeout, wsClient).awaitConnected()
        }
    }

    // backwards compatibility
    fun nonBlocking(
        uri: Uri,
        headers: Headers = emptyList(),
        timeout: Duration = Duration.ZERO,
        wsClient: WebSocketClient = WebSocketClient(defaultJettyHttpClient()
        ), onError: (Throwable) -> Unit = {}, onConnect: WsConsumer = {}) =
        JettyWebsocketClient(timeout, wsClient).nonBlocking(uri, headers, onError, onConnect)

    fun blocking(
        uri: Uri,
        headers: Headers = emptyList(),
        timeout: Duration = Duration.of(5, ChronoUnit.SECONDS)
    ) = JettyWebsocketClient(timeout = timeout).blocking(uri, headers)
}

private class JettyBlockingWebsocket(
    uri: Uri,
    headers: Headers,
    timeout: Duration,
    client: WebSocketClient
) : WsClient {
    private val connected = CompletableFuture<WsClient>()

    private val queue = LinkedBlockingQueue<() -> WsMessage?>()

    private val websocket =
        JettyNonBlockingWebsocket(uri, headers, timeout, client, connected::completeExceptionally) { ws ->
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

private class JettyNonBlockingWebsocket(
    uri: Uri,
    headers: Headers,
    timeout: Duration,
    client: WebSocketClient,
    onError: (Throwable) -> Unit,
    private val onConnect: WsConsumer
) : PushPullAdaptingWebSocket() {

    private val listener = Listener()

    private val req = Request(GET, uri).headers(headers)

    init {
        onError(onError)
        client.connect(listener, URI.create(uri.toString()), clientUpgradeRequest(headers, timeout))
            .whenComplete { _, error -> triggerError(error) }
    }

    override fun send(message: WsMessage) = with(listener) {
        if (!isOpen) {
            throw WebSocketException("Connection to ${req.uri} is closed.")
        }
        try {
            when (message.mode) {
                WsMessage.Mode.Binary -> Completable.with { session.sendBinary(message.body.payload, it) }.get()
                WsMessage.Mode.Text -> Completable.with { session.sendText(message.body.toString(), it) }.get()
            }
        } catch (error: Throwable) {
            triggerError(error)
        }
        Unit
    }

    override fun close(status: WsStatus) = with(listener) {
        if (isOpen) {
            Completable.with { session.close(status.code, status.description, it) }.get()
        }
    }

    inner class Listener : Session.Listener.AbstractAutoDemanding() {
        override fun onWebSocketOpen(session: Session) {
            super.onWebSocketOpen(session)
            onConnect(this@JettyNonBlockingWebsocket)
        }

        override fun onWebSocketClose(statusCode: Int, reason: String?) {
            triggerClose(WsStatus(statusCode, reason.orEmpty()))
        }

        override fun onWebSocketText(message: String) {
            try {
                triggerMessage(WsMessage(message))
            } catch (e: Throwable) {
                triggerError(e)
            }
        }

        override fun onWebSocketBinary(payload: ByteBuffer, callback: Callback) {
            try {
                triggerMessage(WsMessage(BufferUtil.toArray(payload).inputStream()))
                callback.succeed()
            } catch (e: Throwable) {
                triggerError(e)
                callback.fail(e)
            }
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
