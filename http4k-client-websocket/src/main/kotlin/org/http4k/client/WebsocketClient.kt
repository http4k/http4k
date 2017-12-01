package org.http4k.client

import org.http4k.core.Body
import org.http4k.core.Method.GET
import org.http4k.core.Request
import org.http4k.core.Status
import org.http4k.core.StreamBody
import org.http4k.core.Uri
import org.http4k.testing.WsClient
import org.http4k.websocket.PushPullAdaptingWebSocket
import org.http4k.websocket.Websocket
import org.http4k.websocket.WsMessage
import org.java_websocket.client.WebSocketClient
import org.java_websocket.handshake.ServerHandshake
import java.lang.Exception
import java.net.URI
import java.nio.ByteBuffer
import java.util.concurrent.LinkedBlockingQueue
import java.util.concurrent.atomic.AtomicReference

object WebsocketClient {

    fun nonBlocking(uri: Uri): Websocket {
        val socket = AtomicReference<PushPullAdaptingWebSocket>()
        val client = object : WebSocketClient(URI.create(uri.toString())) {
            override fun onOpen(handshakedata: ServerHandshake?) {}

            override fun onClose(code: Int, reason: String, remote: Boolean) = socket.get().triggerClose(Status(code, reason))

            override fun onMessage(message: String) = socket.get().triggerMessage(WsMessage(message))

            override fun onError(ex: Exception) = socket.get().triggerError(ex)

        }
        socket.set(object : PushPullAdaptingWebSocket(Request(GET, uri)) {
            override fun send(message: WsMessage) =
                when (message.body) {
                    is StreamBody -> client.send(message.body.payload)
                    else -> client.send(message.bodyString())
                }

            override fun close(status: Status) {
                client.close(status.code, status.description)
            }
        })
        client.connect()

        return socket.get()
    }

    fun blocking(uri: Uri): WsClient {
        val queue = LinkedBlockingQueue<() -> WsMessage?>()

        val client = object : WebSocketClient(URI.create(uri.toString())) {
            override fun onOpen(handshakedata: ServerHandshake) {}

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
            override val received = generateSequence { queue.take()() }

            override fun close(status: Status) = client.close(status.code, status.description)

            override fun send(message: WsMessage): Unit =
                when (message.body) {
                    is StreamBody -> client.send(message.body.payload)
                    else -> client.send(message.bodyString())
                }
        }
    }
}
