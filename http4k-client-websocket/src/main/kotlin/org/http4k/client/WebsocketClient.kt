package org.http4k.client

import org.http4k.core.Body
import org.http4k.core.Status
import org.http4k.core.StreamBody
import org.http4k.core.Uri
import org.http4k.testing.WsClient
import org.http4k.websocket.WsMessage
import org.java_websocket.client.WebSocketClient
import org.java_websocket.handshake.ServerHandshake
import java.lang.Exception
import java.net.URI
import java.nio.ByteBuffer
import java.util.concurrent.LinkedBlockingQueue

object WebsocketClient {

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

            override fun error(throwable: Throwable) = client.onError(throwable as Exception)

            override fun close(status: Status) = client.close(status.code, status.description)

            override fun send(message: WsMessage): Unit =
                when (message.body) {
                    is StreamBody -> client.send(message.body.payload)
                    else -> client.send(message.bodyString())
                }
        }
    }
}
