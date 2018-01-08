package org.http4k.client

import org.http4k.core.Body
import org.http4k.core.Method.GET
import org.http4k.core.Request
import org.http4k.core.StreamBody
import org.http4k.core.Uri
import org.http4k.testing.WsClient
import org.http4k.websocket.PushPullAdaptingWebSocket
import org.http4k.websocket.Websocket
import org.http4k.websocket.WsConsumer
import org.http4k.websocket.WsMessage
import org.http4k.websocket.WsStatus
import org.java_websocket.client.WebSocketClient
import org.java_websocket.drafts.Draft_6455
import org.java_websocket.handshake.ServerHandshake
import java.lang.Exception
import java.net.URI
import java.nio.ByteBuffer
import java.util.concurrent.LinkedBlockingQueue
import java.util.concurrent.atomic.AtomicReference

object WebsocketClient {

    /**
     * Provides a client-side Websocket instance connected to a remote Websocket. The resultant object
     * can be have listeners attached to it. Optionally pass a WsConsumer which will be called onConnect
     */
    fun nonBlocking(uri: Uri, headers: Map<String, String> = mapOf(), timeoutInMillis: Int = 0, onConnect: WsConsumer = {}): Websocket {
        val socket = AtomicReference<PushPullAdaptingWebSocket>()
        val client = object : WebSocketClient(URI.create(uri.toString()), Draft_6455(), headers, timeoutInMillis) {
            override fun onOpen(handshakedata: ServerHandshake?) {
                onConnect(socket.get())
            }

            override fun onClose(code: Int, reason: String, remote: Boolean) = socket.get().triggerClose(WsStatus(code, reason))

            override fun onMessage(message: String) = socket.get().triggerMessage(WsMessage(message))

            override fun onError(ex: Exception) = socket.get().triggerError(ex)

        }
        socket.set(object : PushPullAdaptingWebSocket(Request(GET, uri)) {
            override fun send(message: WsMessage) =
                    when (message.body) {
                        is StreamBody -> client.send(message.body.payload)
                        else -> client.send(message.bodyString())
                    }

            override fun close(status: WsStatus) {
                client.close(status.code, status.description)
            }
        })
        client.connect()

        return socket.get()
    }

    /**
     * Provides a client-side WsClient connected to a remote Websocket. This is a blocking API, so accessing the sequence of "received"
     * messages will block on iteration until all messages are received (or the socket it closed). This call will also
     * block while connection happens.
     */
    fun blocking(uri: Uri, headers: Map<String, String> = mapOf(), timeoutInMillis: Int = 0): WsClient {
        val queue = LinkedBlockingQueue<() -> WsMessage?>()

        val client = object : WebSocketClient(URI.create(uri.toString()), Draft_6455(), headers, timeoutInMillis) {
            override fun onOpen(sh: ServerHandshake) {}

            override fun onClose(code: Int, reason: String?, remote: Boolean) {
                queue.add({ null })
            }

            override fun onMessage(message: String) {
                queue.add({ WsMessage(message) })
            }

            override fun onMessage(bytes: ByteBuffer) {
                queue.add({ WsMessage(Body(bytes.array().inputStream())) })
            }

            override fun onError(ex: Exception) {
                throw ex
            }
        }

        client.connectBlocking()

        return object : WsClient {
            override fun received() = generateSequence { queue.take()() }

            override fun close(status: WsStatus) = client.close(status.code, status.description)

            override fun send(message: WsMessage): Unit =
                    when (message.body) {
                        is StreamBody -> client.send(message.body.payload)
                        else -> client.send(message.bodyString())
                    }
        }
    }
}
