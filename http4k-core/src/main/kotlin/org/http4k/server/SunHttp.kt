package org.http4k.server

import com.sun.net.httpserver.HttpServer
import org.http4k.core.HttpHandler
import org.http4k.server.ServerConfig.StopMode
import java.net.InetSocketAddress
import java.util.concurrent.Executors.newWorkStealingPool
import java.util.concurrent.TimeUnit

class SunHttp(val port: Int = 8000, override val stopMode: StopMode = StopMode.Immediate) : ServerConfig {
    constructor(port: Int = 8000): this(port, StopMode.Immediate)

    override fun toServer(http: HttpHandler): Http4kServer = object : Http4kServer {
        override fun port(): Int = if (port > 0) port else server.address.port

        private val executor = newWorkStealingPool()
        private val server = HttpServer.create(InetSocketAddress(port), 1000)
        override fun start(): Http4kServer = apply {
            server.createContext("/", HttpExchangeHandler(http))
            server.executor = executor
            server.start()
        }

        override fun stop() = apply {
            if (stopMode is StopMode.Graceful) {
                executor.shutdown()
                executor.awaitTermination(stopMode.timeout.toMillis(), TimeUnit.MILLISECONDS)
            }
            server.stop(0)
        }
    }
}
