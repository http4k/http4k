package org.http4k.server

import org.eclipse.jetty.alpn.server.ALPNServerConnectionFactory
import org.eclipse.jetty.http2.HTTP2Cipher
import org.eclipse.jetty.http2.server.HTTP2ServerConnectionFactory
import org.eclipse.jetty.server.HttpConfiguration
import org.eclipse.jetty.server.SecureRequestCustomizer
import org.eclipse.jetty.server.Server
import org.eclipse.jetty.server.ServerConnector
import org.eclipse.jetty.server.SslConnectionFactory
import org.eclipse.jetty.server.handler.HandlerWrapper
import org.eclipse.jetty.server.handler.StatisticsHandler
import org.eclipse.jetty.servlet.ServletContextHandler
import org.eclipse.jetty.servlet.ServletContextHandler.SESSIONS
import org.eclipse.jetty.servlet.ServletHolder
import org.eclipse.jetty.util.ssl.SslContextFactory
import org.eclipse.jetty.websocket.core.FrameHandler
import org.eclipse.jetty.websocket.core.WebSocketComponents
import org.eclipse.jetty.websocket.core.server.WebSocketNegotiation
import org.eclipse.jetty.websocket.core.server.WebSocketNegotiator
import org.eclipse.jetty.websocket.core.server.WebSocketUpgradeHandler
import org.http4k.core.HttpHandler
import org.http4k.server.ServerConfig.StopMode.Graceful
import org.http4k.servlet.jakarta.asHttp4kRequest
import org.http4k.servlet.jakarta.asServlet
import org.http4k.sse.SseHandler
import org.http4k.websocket.WsHandler
import java.time.Duration.ofSeconds

fun HttpHandler.toJettyHandler(withStatisticsHandler: Boolean = false): HandlerWrapper = ServletContextHandler(
    SESSIONS
).apply {
    addServlet(ServletHolder(this@toJettyHandler.asServlet()), "/*")
}.let {
    if (withStatisticsHandler) StatisticsHandler().apply { handler = it } else it
}

fun SseHandler.toJettySseHandler() = JettyEventStreamHandler(this)

fun WsHandler.toJettyWsHandler() = WebSocketUpgradeHandler(WebSocketComponents()).apply {
    addMapping("/*", this@toJettyWsHandler.toJettyNegotiator())
}

fun WsHandler.toJettyNegotiator() = object : WebSocketNegotiator.AbstractNegotiator() {
    override fun negotiate(negotiation: WebSocketNegotiation): FrameHandler {
        val consumer = this@toJettyNegotiator(negotiation.request.asHttp4kRequest()!!)
        consumer.subprotocol?.also { negotiation.subprotocol = it }
        return Http4kWebSocketFrameHandler(consumer)
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
