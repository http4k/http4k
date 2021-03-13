package org.http4k.serverless.lambda.testing

import org.http4k.core.HttpHandler
import org.http4k.server.Http4kServer
import org.http4k.server.ServerConfig

private object NoOpHttp4kServer : Http4kServer {
    override fun start(): Http4kServer = this
    override fun stop(): Http4kServer = this
    override fun port(): Int = 0
}

internal object NoOpServerConfig : ServerConfig {
    override fun toServer(http: HttpHandler): Http4kServer = NoOpHttp4kServer
}
