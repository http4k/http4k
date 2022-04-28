package org.http4k.testing

import org.http4k.cloudnative.env.Environment
import org.http4k.cloudnative.env.EnvironmentKey
import org.http4k.lens.enum
import org.http4k.server.ServerConfig.StopMode
import org.http4k.server.asServer
import org.http4k.testing.TestServerEvent.ServerStarted
import org.http4k.testing.TestServerEvent.ServerStopRequested
import org.http4k.testing.TestServerEvent.ServerStopped
import java.time.Duration

fun main() {
    val events = ContainerEvents()
    val backendKey = EnvironmentKey.enum({ ServerBackend.valueOf(it) }, ServerBackend::name).required("BACKEND")
    val stopModeKey = EnvironmentKey.map({ resolveStopMode(it) }, { it.javaClass.simpleName }).required("STOP_MODE")

    val environment = Environment.ENV
    val selectedBackend = backendKey(environment)
    val selectedStopMode = stopModeKey(environment)

    val server = ShutdownTestApp().asServer(selectedBackend(selectedStopMode))
        .apply {
            start()
            events(ServerStarted(selectedBackend.name, selectedStopMode::class.java.simpleName))
        }

    Runtime.getRuntime().addShutdownHook(Thread {
        events(ServerStopRequested())
        server.stop()
        events(ServerStopped())
    })
}

private fun resolveStopMode(simpleClassName: String) = when (simpleClassName) {
    StopMode.Immediate::class.java.simpleName -> StopMode.Immediate
    StopMode.Graceful::class.java.simpleName -> StopMode.Graceful(Duration.ofSeconds(10))
    else -> error("Unrecognised stop mode: $simpleClassName")
}

