package org.http4k.client.internal

import org.http4k.core.Body
import org.http4k.core.Headers
import org.http4k.core.StreamBody
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

class AdaptingWebSocket(private val client: WebSocketClient) : PushPullAdaptingWebSocket() {
    override fun send(message: WsMessage) =
        when (message.body) {
            is StreamBody -> client.send(message.body.payload)
            else -> client.send(message.bodyString())
        }

    override fun close(status: WsStatus) = client.close(status.code, status.description)
}

class BlockingQueueClient(
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
        queue += { WsMessage(Body(bytes.array().inputStream())) }
    }

    override fun onError(e: Exception): Unit = throw e
}

class NonBlockingClient(
    uri: Uri,
    headers: Headers,
    timeout: Duration,
    private val onConnect: WsConsumer,
    draft: Draft,
    private val socket: AtomicReference<PushPullAdaptingWebSocket>
) : WebSocketClient(URI.create(uri.toString()), draft, headers.combineToMap(), timeout.toMillis().toInt()) {
    override fun onOpen(handshakedata: ServerHandshake?) {
        onConnect(socket.get())
    }

    override fun onClose(code: Int, reason: String, remote: Boolean) = socket.get().triggerClose(WsStatus(code, reason))

    override fun onMessage(message: String) = socket.get().triggerMessage(WsMessage(message))

    override fun onMessage(bytes: ByteBuffer) =
        socket.get().triggerMessage(WsMessage(Body(bytes.array().inputStream())))

    override fun onError(e: Exception) = socket.get().triggerError(e)
}

class BlockingWsClient(
    private val queue: LinkedBlockingQueue<() -> WsMessage?>,
    private val client: BlockingQueueClient,
    private val autoReconnection: Boolean
) : WsClient {
    override fun received() = generateSequence { queue.take()() }

    override fun close(status: WsStatus) = client.close(status.code, status.description)

    override fun send(message: WsMessage) {
        if (autoReconnection && (client.isClosing || client.isClosed)) {
            client.closeBlocking() // ensure it's closed
            client.reconnectBlocking()
        }

        return when (message.body) {
            is StreamBody -> client.send(message.body.payload)
            else -> client.send(message.bodyString())
        }
    }
}

private fun Headers.combineToMap() = groupBy { it.first }.mapValues { it.value.map { it.second }.joinToString(", ") }
