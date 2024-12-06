package org.http4k.server

import org.http4k.core.HttpHandler
import org.http4k.server.ServerConfig.StopMode
import org.http4k.server.ServerConfig.StopMode.Immediate
import java.util.concurrent.Executors.newWorkStealingPool

/**
 * Stock version of an SunHttp Server. Not that if you want to configure your own server instance you
 * can duplicate this code and modify it as required. We are purposefully trying to limit options
 * here to keep the API simple for the 99% of use-cases.
 */
class SunHttp(private val port: Int = 8000, override val stopMode: StopMode = Immediate) : ServerConfig {
    constructor(port: Int = 8000) : this(port, Immediate)

    override fun toServer(http: HttpHandler) = SunHttpServer(http, port, stopMode, newWorkStealingPool())
}
