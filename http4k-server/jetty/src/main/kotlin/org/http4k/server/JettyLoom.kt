package org.http4k.server

import org.eclipse.jetty.server.Server
import org.http4k.server.ServerConfig.StopMode
import java.time.Duration

fun JettyLoom(port: Int) = JettyLoom(port, defaultStopMode)

fun JettyLoom(port: Int, stopMode: StopMode) = Jetty(
    port, stopMode,
    Server(LoomThreadPool()).apply { addConnector(http(port)(this)) }
)

internal val defaultStopMode = StopMode.Graceful(Duration.ofSeconds(5))
