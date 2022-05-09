package org.http4k.server

import org.http4k.core.HttpHandler
import java.time.Duration

interface Http4kServer : AutoCloseable {
    fun start(): Http4kServer
    fun stop(): Http4kServer
    fun block() = Thread.currentThread().join()
    override fun close() {
        stop()
    }

    fun port(): Int
}

/**
 * Standard interface for creating a configured WebServer
 */
interface ServerConfig {
    sealed class StopMode {
        object Immediate : StopMode()
        data class Graceful(val timeout: Duration): StopMode()
    }

    class UnsupportedStopMode(stopMode: StopMode)
        : IllegalArgumentException("Server does not support stop mode $stopMode")

    val stopMode: StopMode get() = StopMode.Immediate

    fun toServer(http: HttpHandler): Http4kServer
}

fun HttpHandler.asServer(config: ServerConfig): Http4kServer = config.toServer(this)
