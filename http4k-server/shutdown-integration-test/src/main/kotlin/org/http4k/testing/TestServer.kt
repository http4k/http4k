package org.http4k.testing

import org.http4k.cloudnative.env.Environment
import org.http4k.cloudnative.env.EnvironmentKey
import org.http4k.lens.enum
import org.http4k.server.Apache4Server
import org.http4k.server.ApacheServer
import org.http4k.server.Jetty
import org.http4k.server.KtorCIO
import org.http4k.server.KtorNetty
import org.http4k.server.Netty
import org.http4k.server.Ratpack
import org.http4k.server.ServerConfig
import org.http4k.server.ServerConfig.StopMode
import org.http4k.server.SunHttp
import org.http4k.server.Undertow
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

    val app = TestApp().allRoutes

    val server = app.asServer(selectedBackend(selectedStopMode))
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

fun resolveStopMode(simpleClassName: String) = when (simpleClassName) {
    StopMode.Immediate::class.java.simpleName -> StopMode.Immediate
    StopMode.Delayed::class.java.simpleName -> StopMode.Delayed(Duration.ofSeconds(10))
    StopMode.Graceful::class.java.simpleName -> StopMode.Graceful(Duration.ofSeconds(10))
    else -> error("Unrecognised stop mode: $simpleClassName")
}

enum class ServerBackend : (StopMode) -> ServerConfig {
    Apache {
        override fun invoke(mode: StopMode) = ApacheServer(8000, stopMode = mode)
    },
    Apache4 {
        override fun invoke(mode: StopMode) = Apache4Server(8000, stopMode = mode)
    },
    KtorCIO {
        override fun invoke(mode: StopMode) = KtorCIO(8000, stopMode = mode)
    },
    KtorNetty {
        override fun invoke(mode: StopMode) = KtorNetty(8000, stopMode = mode)
    },
    Netty {
        override fun invoke(mode: StopMode) = Netty(8000, stopMode = mode)
    },
    Undertow {
        override fun invoke(mode: StopMode) = Undertow(port = 8000, enableHttp2 = false, mode)
    },
    Ratpack {
        override fun invoke(mode: StopMode) = Ratpack(port = 8000, stopMode = mode)
    },
    SunHttp {
        override fun invoke(mode: StopMode) = SunHttp(port = 8000, stopMode = mode)
    },
    Jetty {
        override fun invoke(mode: StopMode) = Jetty(port = 8000, stopMode = mode)
    }
}
