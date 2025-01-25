package org.http4k.server

import org.http4k.core.HttpHandler
import org.http4k.server.ServerConfig.StopMode
import org.http4k.server.ServerConfig.StopMode.Immediate
import java.util.concurrent.Executors.newWorkStealingPool

class SunHttp(
    val port: Int = 8000,
    override val stopMode: StopMode = Immediate
) : ServerConfig {
    constructor(port: Int = 8000) : this(port, Immediate)

    override fun toServer(http: HttpHandler) = SunHttpServer(http, port, stopMode, newWorkStealingPool())
}
