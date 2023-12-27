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
import org.eclipse.jetty.server.handler.ContextHandler
import org.eclipse.jetty.server.handler.StatisticsHandler
import org.eclipse.jetty.util.ssl.SslContextFactory
import org.eclipse.jetty.websocket.server.WebSocketUpgradeHandler
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

fun WsHandler.toJettyWsHandler(server: Server): Wrapper {
    val contextHandler = ContextHandler("/")
    val wsUpgradeHandler = WebSocketUpgradeHandler.from(server, contextHandler) { container ->
        container.addMapping("/*") { request, _, _ ->
            request.asHttp4kRequest()?.let { http4kRequest ->
                val consumer = this.invoke(http4kRequest)
                if (consumer.subprotocol == null || request.hasSubProtocol(consumer.subprotocol)) {
                    Http4kJettyServerWebSocketEndpoint(consumer, http4kRequest)
                } else {
                    null
                }
            }
        }
    }
    contextHandler.handler = wsUpgradeHandler

    return contextHandler
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
