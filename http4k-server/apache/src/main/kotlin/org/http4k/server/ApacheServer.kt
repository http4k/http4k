package org.http4k.server

import org.apache.hc.core5.http.impl.bootstrap.HttpServer
import org.apache.hc.core5.http.impl.bootstrap.ServerBootstrap
import org.apache.hc.core5.http.io.SocketConfig
import org.apache.hc.core5.io.CloseMode.IMMEDIATE
import org.apache.hc.core5.util.TimeValue
import org.http4k.core.HttpHandler
import org.http4k.server.ServerConfig.StopMode
import java.net.InetAddress
import java.time.Duration

class ApacheServer(
    val port: Int = 8000,
    val address: InetAddress? = null,
    private val canonicalHostname: String? = null,
    override val stopMode: StopMode = StopMode.Graceful(Duration.ofSeconds(5))
) : ServerConfig {

    constructor(port: Int = 8000) : this(port, null, null)
    constructor(port: Int = 8000, address: InetAddress? = null, canonicalHostname: String? = null) : this (port, address, canonicalHostname, StopMode.Immediate)

    override fun toServer(http: HttpHandler): Http4kServer = object : Http4kServer {
        private val server: HttpServer

        init {
            val bootstrap = ServerBootstrap.bootstrap()
                .setListenerPort(port)
                .setSocketConfig(
                    SocketConfig.custom()
                        .setTcpNoDelay(true)
                        .setSoKeepAlive(true)
                        .setSoReuseAddress(true)
                        .setBacklogSize(1000)
                        .build()
                )
                .register("*", Http4kRequestHandler(http))

            if (canonicalHostname != null)
                bootstrap.setCanonicalHostName(canonicalHostname)

            if (address != null)
                bootstrap.setLocalAddress(address)

            server = bootstrap.create()
        }

        override fun start() = apply { server.start() }

        override fun stop() = apply {
            when (stopMode) {
                is StopMode.Immediate -> server.close(IMMEDIATE)
                is StopMode.Graceful -> {
                    server.initiateShutdown()
                    try {
                        server.awaitTermination(TimeValue.ofMilliseconds(stopMode.timeout.toMillis()))
                    } catch (ex: InterruptedException) {
                        Thread.currentThread().interrupt()
                    }
                    server.close(IMMEDIATE)
                }
            }
        }

        override fun port(): Int = if (port != 0) port else server.localPort
    }
}
