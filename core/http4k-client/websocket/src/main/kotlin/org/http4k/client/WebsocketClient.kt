package org.http4k.client

import org.http4k.client.internal.AdaptingWebSocket
import org.http4k.client.internal.BlockingQueueClient
import org.http4k.client.internal.blockingWsClient
import org.http4k.client.internal.nonBlockingClient
import org.http4k.core.Headers
import org.http4k.core.Uri
import org.http4k.websocket.PushPullAdaptingWebSocket
import org.http4k.websocket.Websocket
import org.http4k.websocket.WebsocketFactory
import org.http4k.websocket.WsClient
import org.http4k.websocket.WsConsumer
import org.http4k.websocket.WsMessage
import org.java_websocket.drafts.Draft
import org.java_websocket.drafts.Draft_6455
import org.java_websocket.exceptions.WebsocketNotConnectedException
import java.time.Duration
import java.util.concurrent.LinkedBlockingQueue
import java.util.concurrent.TimeUnit.MILLISECONDS
import java.util.concurrent.atomic.AtomicReference

object WebsocketClient {

    operator fun invoke(
        timeout: Duration = Duration.ofSeconds(5),
        autoReconnection: Boolean = false,
        draft: Draft = Draft_6455(),
    ) = object: WebsocketFactory {

        override fun nonBlocking(uri: Uri, headers: Headers, onError: (Throwable) -> Unit, onConnect: WsConsumer): Websocket {
            val socket = AtomicReference<PushPullAdaptingWebSocket>()
            val client = nonBlockingClient(uri, headers, timeout, onConnect, draft, socket)
            socket.set(AdaptingWebSocket(client, autoReconnection).apply { onError(onError) })
            client.connect()

            return socket.get()
        }

        override fun blocking(uri: Uri, headers: Headers): WsClient {
            val queue = LinkedBlockingQueue<() -> WsMessage?>()
            val client = BlockingQueueClient(uri, headers, timeout, draft, queue).apply {
                if (!connectBlocking(timeout.toMillis(), MILLISECONDS)) {
                    throw WebsocketNotConnectedException()
                }
            }
            return blockingWsClient(queue, client, autoReconnection)
        }
    }

    // backwards compatibility
    fun nonBlocking(
        uri: Uri,
        headers: Headers = emptyList(),
        timeout: Duration = Duration.ofSeconds(5),
        onError: (Throwable) -> Unit = {},
        draft: Draft = Draft_6455(),
        onConnect: WsConsumer = {}
    ) = WebsocketClient(timeout, false, draft).nonBlocking(uri, headers, onError, onConnect)

    fun blocking(
        uri: Uri,
        headers: Headers = emptyList(),
        timeout: Duration = Duration.ofSeconds(5),
        autoReconnection: Boolean = false,
        draft: Draft = Draft_6455(),
    ) = WebsocketClient(timeout, autoReconnection, draft).blocking(uri, headers)
}
