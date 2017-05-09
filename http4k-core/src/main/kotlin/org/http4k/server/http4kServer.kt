package org.http4k.server

import org.http4k.core.HttpHandler

interface Http4kServer {
    fun start(): Http4kServer
    fun stop()
    fun block(): Http4kServer
}

interface ServerConfig {
    fun toServer(handler: HttpHandler): Http4kServer
}

fun HttpHandler.asServer(config: ServerConfig): Http4kServer = config.toServer(this)

fun HttpHandler.startServer(config: ServerConfig, block: Boolean = true): Http4kServer {
    val server = config.toServer(this).start()
    return if (block) server.block() else server
}
