package org.http4k.server

import org.http4k.core.HttpHandler

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
fun interface ServerConfig {
    fun toServer(http: HttpHandler): Http4kServer
}

fun HttpHandler.asServer(config: ServerConfig): Http4kServer = config.toServer(this)
