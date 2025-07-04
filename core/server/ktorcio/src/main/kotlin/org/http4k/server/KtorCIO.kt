package org.http4k.server

import io.ktor.server.application.createApplicationPlugin
import io.ktor.server.application.install
import io.ktor.server.cio.CIO
import io.ktor.server.engine.embeddedServer
import org.http4k.bridge.asHttp4k
import org.http4k.bridge.fromHttp4K
import org.http4k.core.HttpHandler
import org.http4k.core.Response
import org.http4k.core.Status.Companion.NOT_IMPLEMENTED
import org.http4k.server.ServerConfig.StopMode.Immediate
import java.util.concurrent.TimeUnit.MILLISECONDS


@Suppress("EXPERIMENTAL_API_USAGE")
class KtorCIO(val port: Int = 8000, override val stopMode: ServerConfig.StopMode) : ServerConfig {
    constructor(port: Int = 8000) : this(port, Immediate)

    init {
        if (stopMode != Immediate) {
            throw ServerConfig.UnsupportedStopMode(stopMode)
        }
    }

    override fun toServer(http: HttpHandler): Http4kServer = object : Http4kServer {
        private val engine = embeddedServer(CIO, port) {
            install(createApplicationPlugin(name = "http4k") {
                onCall {
                    it.response.fromHttp4K(it.request.asHttp4k()?.let(http) ?: Response(NOT_IMPLEMENTED))
                }
            })
        }

        override fun start() = apply {
            engine.start()
        }

        override fun stop() = apply {
            engine.stop(0, 0, MILLISECONDS)
        }

        override fun port() = engine.engineConfig.connectors.first().port
    }
}
