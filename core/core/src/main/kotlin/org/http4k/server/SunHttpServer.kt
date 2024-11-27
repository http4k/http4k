package org.http4k.server

import com.sun.net.httpserver.HttpServer
import org.http4k.core.HttpHandler
import java.net.InetSocketAddress
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors.newWorkStealingPool
import java.util.concurrent.TimeUnit.MILLISECONDS

internal fun SunHttpServer(
    http: HttpHandler,
    port: Int,
    stopMode: ServerConfig.StopMode,
    executor: ExecutorService = newWorkStealingPool()
) = object : Http4kServer {
    override fun port(): Int = if (port > 0) port else server.address.port

    private val server = HttpServer.create(InetSocketAddress(port), 1000)

    override fun start(): Http4kServer = apply {
        server.createContext("/", HttpExchangeHandler(http))
        server.executor = executor
        server.start()
    }

    override fun stop() = apply {
        if (stopMode is ServerConfig.StopMode.Graceful) {
            executor.shutdown()
            executor.awaitTermination(stopMode.timeout.toMillis(), MILLISECONDS)
        }
        server.stop(0)
    }
}
