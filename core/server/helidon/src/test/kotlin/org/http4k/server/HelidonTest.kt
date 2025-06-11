package org.http4k.server

import org.http4k.server.ServerConfig.StopMode
import org.http4k.server.ServerConfig.StopMode.Graceful
import java.time.Duration.ofMillis

private val defaultStopMode = Graceful(ofMillis(1))
private fun HelidonWithGracefulShutdown(port: Int, ignored: StopMode) = Helidon(port, defaultStopMode)

class HelidonTest : ServerContract(::Helidon, ClientForServerTesting())
class HelidonGracefulTest : ServerContract(::HelidonWithGracefulShutdown, ClientForServerTesting())
