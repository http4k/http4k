package org.http4k.server

import org.apache.hc.core5.http.impl.bootstrap.HttpServer
import org.apache.hc.core5.http.impl.bootstrap.ServerBootstrap
import org.apache.hc.core5.http.impl.routing.RequestRouter
import org.apache.hc.core5.http.io.HttpRequestHandler
import org.apache.hc.core5.http.io.SocketConfig
import org.apache.hc.core5.io.CloseMode.IMMEDIATE
import org.apache.hc.core5.net.URIAuthority
import org.apache.hc.core5.util.TimeValue
import org.http4k.core.HttpHandler
import org.http4k.server.ServerConfig.StopMode
import java.time.Duration

class ApacheServer(
    val port: Int,
    override val stopMode: StopMode
) : ServerConfig {
    constructor(port: Int = 8000) : this(port, StopMode.Graceful(Duration.ofSeconds(5)))

    private val canonicalHostname = "localhost"

    override fun toServer(http: HttpHandler): Http4kServer = object : Http4kServer {
        private val server = defaultBootstrap(port, http, canonicalHostname).create()

        override fun start() = apply { server.start() }

        override fun stop() = apply {
            server.stopWith(stopMode)
        }

        override fun port(): Int = if (port != 0) port else server.localPort
    }
}

fun HttpServer.stopWith(stopMode: StopMode) = when (stopMode) {
    is StopMode.Immediate -> close(IMMEDIATE)
    is StopMode.Graceful -> {
        initiateShutdown()
        try {
            awaitTermination(TimeValue.ofMilliseconds(stopMode.timeout.toMillis()))
        } catch (ex: InterruptedException) {
            Thread.currentThread().interrupt()
        }
        close(IMMEDIATE)
    }
}

fun defaultBootstrap(port: Int, http: HttpHandler, canonicalHostname: String): ServerBootstrap {
    val fallbackAuthority = URIAuthority.create("fallback")

    return ServerBootstrap.bootstrap()
        .setListenerPort(port)
        .setCanonicalHostName(canonicalHostname)
        .setSocketConfig(
            SocketConfig.custom()
                .setTcpNoDelay(true)
                .setSoKeepAlive(true)
                .setSoReuseAddress(true)
                .setBacklogSize(1000)
                .build()
        )
        .setRequestRouter(
            RequestRouter.builder<HttpRequestHandler>()
                .addRoute(fallbackAuthority, "*", Http4kRequestHandler(http))
                .resolveAuthority { _: String, _: URIAuthority -> fallbackAuthority }
                .build())
}
