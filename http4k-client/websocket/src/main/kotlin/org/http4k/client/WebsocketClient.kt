package org.http4k.client

import org.http4k.client.internal.AdaptingWebSocket
import org.http4k.client.internal.BlockingQueueClient
import org.http4k.client.internal.BlockingWsClient
import org.http4k.client.internal.NonBlockingClient
import org.http4k.core.Headers
import org.http4k.core.Uri
import org.http4k.websocket.PushPullAdaptingWebSocket
import org.http4k.websocket.Websocket
import org.http4k.websocket.WsClient
import org.http4k.websocket.WsConsumer
import org.http4k.websocket.WsMessage
import org.java_websocket.drafts.Draft
import org.java_websocket.drafts.Draft_6455
import org.java_websocket.exceptions.WebsocketNotConnectedException
import java.time.Duration
import java.time.Duration.ZERO
import java.time.Duration.of
import java.time.temporal.ChronoUnit.SECONDS
import java.util.concurrent.LinkedBlockingQueue
import java.util.concurrent.TimeUnit.MILLISECONDS
import java.util.concurrent.atomic.AtomicReference

object WebsocketClient {

    /**
     * Provides a client-side Websocket instance connected to a remote Websocket. The resultant object
     * can be have listeners attached to it. Optionally pass a WsConsumer which will be called onConnect
     */
    fun nonBlocking(uri: Uri, headers: Headers = emptyList(), timeout: Duration = ZERO, onError: (Throwable) -> Unit = {}, draft: Draft = Draft_6455(), onConnect: WsConsumer = {}): Websocket {
        val socket = AtomicReference<PushPullAdaptingWebSocket>()
        val client = NonBlockingClient(uri, headers, timeout, onConnect, draft, socket)
        socket.set(AdaptingWebSocket(client).apply { onError(onError) })
        client.connect()
        return socket.get()
    }

    /**
     * Provides a client-side WsClient connected to a remote Websocket. This is a blocking API, so accessing the sequence of "received"
     * messages will block on iteration until all messages are received (or the socket is closed). This call will also
     * block while connection happens.
     */
    fun blocking(uri: Uri, headers: Headers = emptyList(), timeout: Duration = of(5, SECONDS), autoReconnection: Boolean = false, draft: Draft = Draft_6455()): WsClient {
        val queue = LinkedBlockingQueue<() -> WsMessage?>()
        val client = BlockingQueueClient(uri, headers, timeout, draft, queue)
            .apply {
                if (!connectBlocking(timeout.toMillis(), MILLISECONDS)) {
                    throw WebsocketNotConnectedException()
                }
            }
        return BlockingWsClient(queue, client, autoReconnection)
    }
}
