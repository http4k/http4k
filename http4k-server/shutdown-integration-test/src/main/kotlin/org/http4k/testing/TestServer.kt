package org.http4k.testing

import org.http4k.cloudnative.env.Environment
import org.http4k.cloudnative.env.EnvironmentKey
import org.http4k.core.Request
import org.http4k.core.Response
import org.http4k.core.Status.Companion.OK
import org.http4k.lens.enum
import org.http4k.server.ApacheServer
import org.http4k.server.ServerConfig
import org.http4k.server.ServerConfig.StopMode
import org.http4k.server.Undertow
import org.http4k.server.asServer
import org.http4k.testing.TestServerEvent.*
import java.time.Duration

fun main() {
    val events = ContainerEvents()
    val backendKey = EnvironmentKey.enum({ ServerBackend.valueOf(it) }, ServerBackend::name).required("BACKEND")
    val stopModeKey = EnvironmentKey.map({ resolveStopMode(it) }, { it.javaClass.simpleName }).required("STOP_MODE")

    val environment = Environment.ENV
    val selectedBackend = backendKey(environment)
    val selectedStopMode = stopModeKey(environment)

    val app = { _: Request -> Response(OK).body("hello from http4k") }

    val server = app.asServer(selectedBackend(selectedStopMode))
        .apply {
            start()
            events(ServerStarted(selectedBackend))
        }

    Runtime.getRuntime().addShutdownHook(Thread {
        println("shutdown requested")
        events(ServerStopRequested())
        server.stop()
        events(ServerStopped())
    })
}

fun resolveStopMode(simpleClassName: String) = when (simpleClassName) {
    StopMode.Immediate::class.java.simpleName -> StopMode.Immediate
    StopMode.Delayed::class.java.simpleName -> StopMode.Delayed(Duration.ofSeconds(10))
    StopMode.Graceful::class.java.simpleName -> StopMode.Delayed(Duration.ofSeconds(10))
    else -> error("Unrecognised stop mode: $simpleClassName")
}

enum class ServerBackend : (StopMode) -> ServerConfig {
    Apache {
        override fun invoke(mode: StopMode) = ApacheServer(8000, stopMode = mode)
    },
    Undertow {
        override fun invoke(mode: StopMode): ServerConfig = Undertow(port = 8000)
    }
}
