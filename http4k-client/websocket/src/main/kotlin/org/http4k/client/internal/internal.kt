package org.http4k.client.internal

import org.http4k.core.Headers
import org.http4k.core.Uri
import org.http4k.websocket.PushPullAdaptingWebSocket
import org.http4k.websocket.WsClient
import org.http4k.websocket.WsConsumer
import org.http4k.websocket.WsMessage
import org.http4k.websocket.WsStatus
import org.java_websocket.client.WebSocketClient
import org.java_websocket.drafts.Draft
import org.java_websocket.handshake.ServerHandshake
import java.net.URI
import java.nio.ByteBuffer
import java.time.Duration
import java.util.concurrent.LinkedBlockingQueue
import java.util.concurrent.atomic.AtomicReference

internal class AdaptingWebSocket(
    private val client: WebSocketClient,
    private val autoReconnection: Boolean
) : PushPullAdaptingWebSocket() {
    override fun send(message: WsMessage) {
        if (autoReconnection && (client.isClosing || client.isClosed)) {
            client.closeBlocking() // ensure it's closed
            client.reconnectBlocking()
        }
        when (message.mode) {
            WsMessage.Mode.Binary -> client.send(message.body.payload)
            WsMessage.Mode.Text -> client.send(message.bodyString())
        }
    }

    override fun close(status: WsStatus) = client.close(status.code, status.description)
}

internal class BlockingQueueClient(
    uri: Uri,
    headers: Headers,
    timeout: Duration,
    draft: Draft,
    private val queue: LinkedBlockingQueue<() -> WsMessage?>
) : WebSocketClient(URI.create(uri.toString()), draft, headers.combineToMap(), timeout.toMillis().toInt()) {
    override fun onOpen(sh: ServerHandshake) {}

    override fun onClose(code: Int, reason: String?, remote: Boolean) {
        queue += { null }
    }

    override fun onMessage(message: String) {
        queue += { WsMessage(message) }
    }

    override fun onMessage(bytes: ByteBuffer) {
        queue += { WsMessage(bytes) }
    }

    override fun onError(e: Exception): Unit = throw e
}

internal fun nonBlockingClient(
    uri: Uri,
    headers: Headers,
    timeout: Duration,
    onConnect: WsConsumer,
    draft: Draft,
    socket: AtomicReference<PushPullAdaptingWebSocket>,
) = object: WebSocketClient(URI.create(uri.toString()), draft, headers.combineToMap(), timeout.toMillis().toInt()) {
    override fun onOpen(handshakedata: ServerHandshake?) = onConnect(socket.get())

    override fun onClose(code: Int, reason: String, remote: Boolean) = socket.get().triggerClose(WsStatus(code, reason))

    override fun onMessage(message: String) = socket.get().triggerMessage(WsMessage(message))

    override fun onMessage(bytes: ByteBuffer) = socket.get().triggerMessage(WsMessage(bytes))

    override fun onError(e: Exception) = socket.get().triggerError(e)
}

internal fun blockingWsClient(
    queue: LinkedBlockingQueue<() -> WsMessage?>,
    client: BlockingQueueClient,
    autoReconnection: Boolean
) = object:  WsClient {
    override fun received() = generateSequence { queue.take()() }

    override fun close(status: WsStatus) = client.close(status.code, status.description)

    override fun send(message: WsMessage) {
        if (autoReconnection && (client.isClosing || client.isClosed)) {
            client.closeBlocking() // ensure it's closed
            client.reconnectBlocking()
        }

        return when (message.mode) {
            WsMessage.Mode.Binary -> client.send(message.body.payload)
            WsMessage.Mode.Text -> client.send(message.bodyString())
        }
    }
}

private fun Headers.combineToMap() = groupBy { it.first }.mapValues { it.value.map { it.second }.joinToString(", ") }
