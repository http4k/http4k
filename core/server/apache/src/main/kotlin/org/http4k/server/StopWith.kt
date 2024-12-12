package org.http4k.server

import org.apache.hc.core5.http.impl.bootstrap.HttpServer
import org.apache.hc.core5.io.CloseMode
import org.apache.hc.core5.util.TimeValue

fun HttpServer.stopWith(stopMode: ServerConfig.StopMode) = when (stopMode) {
    is ServerConfig.StopMode.Immediate -> close(CloseMode.IMMEDIATE)
    is ServerConfig.StopMode.Graceful -> {
        initiateShutdown()
        try {
            awaitTermination(TimeValue.ofMilliseconds(stopMode.timeout.toMillis()))
        } catch (ex: InterruptedException) {
            Thread.currentThread().interrupt()
        }
        close(CloseMode.IMMEDIATE)
    }
}
