package org.http4k.serverless

import org.http4k.server.Http4kServer

/**
 * Launching point for custom Serverless runtimes.
 */
interface ServerlessConfig {
    fun asServer(app: AppLoader): Http4kServer
}
