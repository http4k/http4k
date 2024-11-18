package org.http4k.server

import org.http4k.core.HttpHandler
import java.util.concurrent.Executors.newVirtualThreadPerTaskExecutor

class SunHttpLoom(
    val port: Int = 8000,
    override val stopMode: ServerConfig.StopMode = ServerConfig.StopMode.Immediate
) : ServerConfig {
    constructor(port: Int = 8000) : this(port, ServerConfig.StopMode.Immediate)

    override fun toServer(http: HttpHandler): Http4kServer =
        SunHttpServer(http, port, stopMode, newVirtualThreadPerTaskExecutor())
}
