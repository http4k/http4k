package org.http4k.serverless

import org.http4k.server.Http4kServer

/**
 * Launching point for custom Serverless runtimes.
 */
interface ServerlessConfig {
    fun <Ctx> asServer(app: FnLoader<Ctx>): Http4kServer
}

fun <Ctx> FnLoader<Ctx>.toServer(config: ServerlessConfig) = config.asServer(this)
