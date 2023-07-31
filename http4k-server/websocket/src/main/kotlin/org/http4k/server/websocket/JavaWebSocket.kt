package org.http4k.server.websocket

import org.http4k.core.HttpHandler
import org.http4k.core.MemoryBody
import org.http4k.core.Method.GET
import org.http4k.core.Request
import org.http4k.core.RequestSource
import org.http4k.core.StreamBody
import org.http4k.server.Http4kServer
import org.http4k.server.PolyServerConfig
import org.http4k.server.ServerConfig
import org.http4k.sse.SseHandler
import org.http4k.websocket.PushPullAdaptingWebSocket
import org.http4k.websocket.WsHandler
import org.http4k.websocket.WsMessage
import org.http4k.websocket.WsStatus
import org.java_websocket.WebSocket
import org.java_websocket.drafts.Draft
import org.java_websocket.handshake.ClientHandshake
import org.java_websocket.server.WebSocketServer
import java.net.InetSocketAddress
import java.nio.ByteBuffer
import java.time.Duration
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

class JavaWebSocket(
    private val port: Int = 8000,
    private val hostName: String = "0.0.0.0",
    private val drafts: List<Draft>? = null,
    override val stopMode: ServerConfig.StopMode = ServerConfig.StopMode.Immediate,
    private val addShutdownHook: Boolean = true,
    private val startupTimeout: Duration = Duration.ofSeconds(5),
    private val configFn: WebSocketServer.() -> Unit = {
        isReuseAddr = true // Set SO_REUSEADDR by default
    },
) : PolyServerConfig {

    override fun toServer(http: HttpHandler?, ws: WsHandler?, sse: SseHandler?): Http4kServer {
        if (http != null) throw UnsupportedOperationException("JavaWebSocket does not support http")
        if (ws == null) throw IllegalStateException("JavaWebSocket requires a WsHandler")
        if (sse != null) throw UnsupportedOperationException("JavaWebSocket does not support sse")

        val startLatch = CountDownLatch(1)
        val address = InetSocketAddress(hostName, port)
        val server = createServer(ws, address, drafts, startLatch::countDown).also(configFn)

        val http4kServer = object : Http4kServer {
            override fun port() = server.port
            override fun start() = also {
                server.start()
                startLatch.await(startupTimeout.toMillis(), TimeUnit.MILLISECONDS)
            }

            override fun stop() = also {
                when (stopMode) {
                    ServerConfig.StopMode.Immediate -> server.stop()
                    is ServerConfig.StopMode.Graceful -> server.stop(stopMode.timeout.toMillis().toInt())
                }
            }
        }

        if (addShutdownHook) {
            Runtime.getRuntime().addShutdownHook(Thread(http4kServer::stop))
        }

        return http4kServer
    }
}

private fun createServer(
    wsHandler: WsHandler,
    address: InetSocketAddress,
    drafts: List<Draft>?,
    onServerStart: () -> Unit
) = object : WebSocketServer(address, drafts) {

    override fun onOpen(conn: WebSocket, handshake: ClientHandshake) {
        val headers = handshake.iterateHttpFields()
            .asSequence()
            .map { it to handshake.getFieldValue(it) }
            .toList()

        val upgradeRequest = Request(GET, handshake.resourceDescriptor)
            .headers(headers)
            .let { if (handshake.content != null) it.body(MemoryBody(handshake.content)) else it }
            .source(RequestSource(conn.remoteSocketAddress.hostString, conn.remoteSocketAddress.port))

        val wsAdapter = object : PushPullAdaptingWebSocket() {
            override fun send(message: WsMessage) {
                when (message.body) {
                    is StreamBody -> conn.send(message.body.payload)
                    else -> conn.send(message.bodyString())  // furthering the generalization that a MemoryBody is ALWAYS to use text mode
                }
            }

            override fun close(status: WsStatus) {
                conn.close(status.code, status.description)
            }
        }

        conn.setAttachment(wsAdapter)
        wsHandler(upgradeRequest)(wsAdapter)
    }

    override fun onClose(conn: WebSocket, code: Int, reason: String?, remote: Boolean) {
        conn.adapter()?.triggerClose(WsStatus(code, reason ?: ""))
    }

    override fun onMessage(conn: WebSocket, message: ByteBuffer) {
        conn.adapter()?.let { ws ->
            try {
                ws.triggerMessage(WsMessage(MemoryBody(message)))
            } catch (e: Throwable) {
                ws.triggerError(e)
            }
        }
    }

    override fun onMessage(conn: WebSocket, message: String) {
        conn.adapter()?.let { ws ->
            try {
                ws.triggerMessage(WsMessage(message))
            } catch (e: Throwable) {
                ws.triggerError(e)
            }
        }
    }

    override fun onError(conn: WebSocket?, ex: Exception) {
        conn?.adapter()?.triggerError(ex)
    }

    override fun onStart() = onServerStart()
}

private fun WebSocket.adapter(): PushPullAdaptingWebSocket? = getAttachment()
