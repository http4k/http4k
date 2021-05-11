package org.http4k.serverless

import org.http4k.server.Http4kServer
import java.io.InputStream

/**
 * Launching point for custom Serverless runtimes.
 */
fun interface ServerlessConfig<Ctx> {
    fun asServer(fn: FnLoader<Ctx>): Http4kServer
}

fun <Ctx> FnHandler<InputStream, Ctx, InputStream>.toServer(config: ServerlessConfig<Ctx>) = config.asServer { this }

fun <Ctx> FnLoader<Ctx>.toServer(config: ServerlessConfig<Ctx>) = config.asServer(this)
