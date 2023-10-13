package org.http4k.server

import org.eclipse.jetty.alpn.server.ALPNServerConnectionFactory
import org.eclipse.jetty.http2.HTTP2Cipher
import org.eclipse.jetty.http2.server.HTTP2ServerConnectionFactory
import org.eclipse.jetty.server.Handler.Wrapper
import org.eclipse.jetty.server.HttpConfiguration
import org.eclipse.jetty.server.SecureRequestCustomizer
import org.eclipse.jetty.server.Server
import org.eclipse.jetty.server.ServerConnector
import org.eclipse.jetty.server.SslConnectionFactory
import org.eclipse.jetty.server.handler.StatisticsHandler
import org.eclipse.jetty.util.Callback
import org.eclipse.jetty.util.ssl.SslContextFactory
import org.eclipse.jetty.websocket.core.FrameHandler
import org.eclipse.jetty.websocket.core.WebSocketComponents
import org.eclipse.jetty.websocket.core.server.ServerUpgradeRequest
import org.eclipse.jetty.websocket.core.server.ServerUpgradeResponse
import org.eclipse.jetty.websocket.core.server.WebSocketNegotiator
import org.eclipse.jetty.websocket.core.server.WebSocketUpgradeHandler
import org.http4k.core.HttpHandler
import org.http4k.server.ServerConfig.StopMode.Graceful
import org.http4k.sse.SseHandler
import org.http4k.websocket.WsHandler
import java.time.Duration.ofSeconds

fun HttpHandler.toJettyHandler(withStatisticsHandler: Boolean = false): Wrapper = Wrapper(
    Http4kJettyHttpHandler(this).let {
        if (withStatisticsHandler) StatisticsHandler().apply { handler = it } else it
    }
)

fun SseHandler.toJettySseHandler(): Wrapper = Wrapper(JettyEventStreamHandler(this))

fun WsHandler.toJettyWsHandler() = WebSocketUpgradeHandler(WebSocketComponents()).apply {
    addMapping("/*", this@toJettyWsHandler.toJettyNegotiator())
}

fun WsHandler.toJettyNegotiator() = object : WebSocketNegotiator.AbstractNegotiator() {
    override fun negotiate(
        request: ServerUpgradeRequest,
        response: ServerUpgradeResponse,
        callback: Callback
    ): FrameHandler? {
        val frameHandler = request.asHttp4kRequest()?.let {
            val consumer = this@toJettyNegotiator(it)
            Http4kWebSocketFrameHandler(consumer).takeUnless {
                val subProtocol = consumer.subprotocol
                subProtocol != null && !request.hasSubProtocol(subProtocol)
            }
        }

        if (frameHandler == null) {
            callback.succeeded()
        }

        return frameHandler
    }
}

typealias ConnectorBuilder = (Server) -> ServerConnector

fun http(httpPort: Int): ConnectorBuilder = { server: Server -> ServerConnector(server).apply { port = httpPort } }

fun http2(http2Port: Int, keystorePath: String, keystorePassword: String): ConnectorBuilder =
    { server: Server ->
        ServerConnector(
            server,
            SslConnectionFactory(
                SslContextFactory.Server().apply {
                    keyStorePath = keystorePath
                    keyStorePassword = keystorePassword
                    cipherComparator = HTTP2Cipher.COMPARATOR
                    provider = "Conscrypt"
                },
                "alpn"
            ),
            ALPNServerConnectionFactory().apply {
                defaultProtocol = "h2"
            },
            HTTP2ServerConnectionFactory(HttpConfiguration().apply {
                sendServerVersion = false
                secureScheme = "https"
                securePort = http2Port
                addCustomizer(SecureRequestCustomizer())
            })
        ).apply { port = http2Port }
    }

internal val defaultStopMode = Graceful(ofSeconds(5))
