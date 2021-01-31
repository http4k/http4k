package org.http4k.server

import org.eclipse.jetty.alpn.server.ALPNServerConnectionFactory
import org.eclipse.jetty.http2.HTTP2Cipher.COMPARATOR
import org.eclipse.jetty.http2.server.HTTP2ServerConnectionFactory
import org.eclipse.jetty.server.HttpConfiguration
import org.eclipse.jetty.server.SecureRequestCustomizer
import org.eclipse.jetty.server.Server
import org.eclipse.jetty.server.ServerConnector
import org.eclipse.jetty.server.SslConnectionFactory
import org.eclipse.jetty.servlet.ServletContextHandler
import org.eclipse.jetty.servlet.ServletContextHandler.SESSIONS
import org.eclipse.jetty.servlet.ServletHolder
import org.eclipse.jetty.util.ssl.SslContextFactory
import org.eclipse.jetty.websocket.core.FrameHandler
import org.eclipse.jetty.websocket.core.WebSocketComponents
import org.eclipse.jetty.websocket.core.server.WebSocketNegotiation
import org.eclipse.jetty.websocket.core.server.WebSocketNegotiator.AbstractNegotiator
import org.eclipse.jetty.websocket.core.server.WebSocketUpgradeHandler
import org.http4k.core.HttpHandler
import org.http4k.servlet.asHttp4kRequest
import org.http4k.servlet.asServlet
import org.http4k.sse.SseHandler
import org.http4k.websocket.WsHandler
import java.lang.UnsupportedOperationException


class Jetty(private val port: Int, private val server: Server) : PolyServerConfig {
    constructor(port: Int = 8000) : this(port, http(port))
    constructor(port: Int, vararg inConnectors: ConnectorBuilder) : this(port, Server().apply {
        inConnectors.forEach { addConnector(it(this)) }
    })

    override fun toServer(http: HttpHandler?, ws: WsHandler?, sse: SseHandler?): Http4kServer {
        if(sse != null) throw UnsupportedOperationException("Jetty does not support sse")
        http?.let { server.insertHandler(http.toJettyHandler()) }
        ws?.let {
            server.insertHandler(
                WebSocketUpgradeHandler(
                    WebSocketComponents()).apply {
                    addMapping("/*", it.toJettyNegotiator())
                })
        }

        return object : Http4kServer {
            override fun start(): Http4kServer = apply {
                server.start()
            }

            override fun stop(): Http4kServer = apply { server.stop() }

            override fun port(): Int = if (port > 0) port else server.uri.port
        }
    }
}

fun WsHandler.toJettyNegotiator() = object : AbstractNegotiator() {
    override fun negotiate(negotiation: WebSocketNegotiation): FrameHandler {
        val request = negotiation.request.asHttp4kRequest()

        return this@toJettyNegotiator(request)?.let { Http4kWebSocketFrameHandler(it, request) }!!
    }
}

fun HttpHandler.toJettyHandler() = ServletContextHandler(SESSIONS).apply {
    addServlet(ServletHolder(this@toJettyHandler.asServlet()), "/*")
}

typealias ConnectorBuilder = (Server) -> ServerConnector

fun http(httpPort: Int): ConnectorBuilder = { server: Server -> ServerConnector(server).apply { port = httpPort } }

fun http2(http2Port: Int, keystorePath: String, keystorePassword: String): ConnectorBuilder =
    { server: Server ->
        ServerConnector(server,
            SslConnectionFactory(
                SslContextFactory.Server().apply {
                    keyStorePath = keystorePath
                    setKeyStorePassword(keystorePassword)
                    cipherComparator = COMPARATOR
                    provider = "Conscrypt"
                },
                "alpn"),
            ALPNServerConnectionFactory().apply {
                defaultProtocol = "h2"
            },
            HTTP2ServerConnectionFactory(HttpConfiguration().apply {
                sendServerVersion = false
                secureScheme = "https"
                securePort = http2Port
                addCustomizer(SecureRequestCustomizer())
            })).apply { port = http2Port }
    }
