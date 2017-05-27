package org.http4k.server

import org.http4k.core.HttpHandler

interface Http4kServer {
    fun startAndBlock() = start().block()
    fun start(): Http4kServer
    fun stop()
    fun block() = Thread.currentThread().join()

}

interface ServerConfig {
    fun toServer(handler: HttpHandler): Http4kServer
}

fun HttpHandler.asServer(config: ServerConfig): Http4kServer = config.toServer(this)

fun HttpHandler.startServer(config: ServerConfig, block: Boolean = true): Http4kServer =
    asServer(config).let {
        if (block) it.startAndBlock()
        it.start()
    }