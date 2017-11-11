package org.http4k.server

import org.http4k.core.HttpHandler

interface Http4kServer {
    fun start(): Http4kServer
    fun stop()
    fun block() = Thread.currentThread().join()
}

interface ServerConfig {
    fun toServer(handler: HttpHandler): Http4kServer
}

fun HttpHandler.asServer(config: ServerConfig): Http4kServer = config.toServer(this)
