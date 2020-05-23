package org.http4k.client

import org.http4k.core.Body
import org.http4k.core.Headers
import org.http4k.core.Method.GET
import org.http4k.core.Request
import org.http4k.core.StreamBody
import org.http4k.core.Uri
import org.http4k.websocket.PushPullAdaptingWebSocket
import org.http4k.websocket.Websocket
import org.http4k.websocket.WsClient
import org.http4k.websocket.WsConsumer
import org.http4k.websocket.WsMessage
import org.http4k.websocket.WsStatus
import org.java_websocket.client.WebSocketClient
import org.java_websocket.drafts.Draft_6455
import org.java_websocket.handshake.ServerHandshake
import java.net.URI
import java.nio.ByteBuffer
import java.time.Duration
import java.time.Duration.ZERO
import java.util.concurrent.LinkedBlockingQueue
import java.util.concurrent.atomic.AtomicReference

object WebsocketClient {

    /**
     * Provides a client-side Websocket instance connected to a remote Websocket. The resultant object
     * can be have listeners attached to it. Optionally pass a WsConsumer which will be called onConnect
     */
    fun nonBlocking(uri: Uri, headers: Headers = emptyList(), timeout: Duration = ZERO, onError: (Throwable) -> Unit = {}, onConnect: WsConsumer = {}): Websocket {
        val socket = AtomicReference<PushPullAdaptingWebSocket>()
        val client = NonBlockingClient(uri, headers, timeout, onConnect, socket)
        socket.set(AdaptingWebSocket(uri, client).apply { onError(onError) })
        client.connect()
        return socket.get()
    }

    /**
     * Provides a client-side WsClient connected to a remote Websocket. This is a blocking API, so accessing the sequence of "received"
     * messages will block on iteration until all messages are received (or the socket it closed). This call will also
     * block while connection happens.
     */
    fun blocking(uri: Uri, headers: Headers = emptyList(), timeout: Duration = ZERO, autoReconnection: Boolean = false): WsClient {
        val queue = LinkedBlockingQueue<() -> WsMessage?>()
        val client = BlockingQueueClient(uri, headers, timeout, queue).apply { connectBlocking() }
        return BlockingWsClient(queue, client, autoReconnection)
    }
}

private fun Headers.combineToMap() = groupBy { it.first }.mapValues { it.value.map { it.second }.joinToString(", ") }

private class AdaptingWebSocket(uri: Uri, private val client: WebSocketClient) : PushPullAdaptingWebSocket(Request(GET, uri)) {
    override fun send(message: WsMessage) =
        when (message.body) {
            is StreamBody -> client.send(message.body.payload)
            else -> client.send(message.bodyString())
        }

    override fun close(status: WsStatus) = client.close(status.code, status.description)
}

private class BlockingQueueClient(uri: Uri, headers: Headers, timeout: Duration, private val queue: LinkedBlockingQueue<() -> WsMessage?>) : WebSocketClient(URI.create(uri.toString()), Draft_6455(), headers.combineToMap(), timeout.toMillis().toInt()) {
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

private class NonBlockingClient(uri: Uri, headers: Headers, timeout: Duration, private val onConnect: WsConsumer, private val socket: AtomicReference<PushPullAdaptingWebSocket>) : WebSocketClient(URI.create(uri.toString()), Draft_6455(), headers.combineToMap(), timeout.toMillis().toInt()) {
    override fun onOpen(handshakedata: ServerHandshake?) {
        onConnect(socket.get())
    }

    override fun onClose(code: Int, reason: String, remote: Boolean) = socket.get().triggerClose(WsStatus(code, reason))

    override fun onMessage(message: String) = socket.get().triggerMessage(WsMessage(message))

    override fun onError(e: Exception) = socket.get().triggerError(e)
}

private class BlockingWsClient(private val queue: LinkedBlockingQueue<() -> WsMessage?>, private val client: BlockingQueueClient, private val autoReconnection: Boolean) : WsClient {
    override fun received() = generateSequence { queue.take()() }

    override fun close(status: WsStatus) = client.close(status.code, status.description)

    override fun send(message: WsMessage): Unit {
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
