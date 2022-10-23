package org.http4k.client

import okhttp3.OkHttpClient
import okhttp3.Response
import okhttp3.WebSocket
import okhttp3.WebSocketListener
import okio.ByteString
import okio.ByteString.Companion.toByteString
import org.http4k.client.PreCannedOkHttpClients.defaultOkHttpClient
import org.http4k.core.Body
import org.http4k.core.Headers
import org.http4k.core.Method
import org.http4k.core.Request
import org.http4k.core.StreamBody
import org.http4k.core.Uri
import org.http4k.websocket.PushPullAdaptingWebSocket
import org.http4k.websocket.Websocket
import org.http4k.websocket.WsClient
import org.http4k.websocket.WsConsumer
import org.http4k.websocket.WsMessage
import org.http4k.websocket.WsStatus
import java.time.Duration
import java.time.temporal.ChronoUnit
import java.util.concurrent.CompletableFuture
import java.util.concurrent.ExecutionException
import java.util.concurrent.LinkedBlockingQueue

object OkHttpWebsocketClient {

    fun blocking(
        uri: Uri,
        headers: Headers = emptyList(),
        timeout: Duration = Duration.of(5, ChronoUnit.SECONDS),
        client: OkHttpClient = defaultOkHttpClient()
    ): WsClient = BlockingQueueWsClient(uri, headers, timeout, client).awaitConnected()

    fun nonBlocking(
        uri: Uri,
        headers: Headers = emptyList(),
        timeout: Duration = Duration.ZERO,
        client: OkHttpClient = defaultOkHttpClient(),
        onError: (Throwable) -> Unit = {},
        onConnect: WsConsumer = {}
    ): Websocket = NonBlockingWebsocket(uri, headers, timeout, client, onError, onConnect)
}

private class BlockingQueueWsClient(
    uri: Uri,
    headers: Headers,
    timeout: Duration,
    client: OkHttpClient
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
    client: OkHttpClient,
    onError: (Throwable) -> Unit,
    private val onConnect: WsConsumer
) : PushPullAdaptingWebSocket(Request(Method.GET, uri).headers(headers)) {

    init {
        onError(onError)
    }

    private val ws = client.newBuilder().connectTimeout(timeout).build()
        .newWebSocket(upgradeRequest.asOkHttp(), Listener())

    override fun send(message: WsMessage) {
        val messageSent = when (message.body) {
            is StreamBody -> ws.send(message.body.payload.toByteString())
            else -> ws.send(message.body.toString())
        }
        check(messageSent) {
            "Connection to ${upgradeRequest.uri} is closed."
        }
    }

    override fun close(status: WsStatus) {
        ws.close(status.code, status.description)
    }

    inner class Listener : WebSocketListener() {
        override fun onOpen(webSocket: WebSocket, response: Response) {
            onConnect(this@NonBlockingWebsocket)
        }

        override fun onClosed(webSocket: WebSocket, code: Int, reason: String) {
            triggerClose(WsStatus(code, reason))
        }

        override fun onClosing(webSocket: WebSocket, code: Int, reason: String) {
            webSocket.close(code, reason)
        }

        override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
            triggerError(t)
        }

        override fun onMessage(webSocket: WebSocket, text: String) {
            triggerMessage(WsMessage(text))
        }

        override fun onMessage(webSocket: WebSocket, bytes: ByteString) {
            triggerMessage(WsMessage(Body(bytes.toByteArray().inputStream())))
        }
    }
}
