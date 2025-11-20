package org.http4k.server

import org.http4k.core.Method
import org.http4k.server.ServerConfig.StopMode
import org.http4k.server.ServerConfig.StopMode.Graceful
import java.time.Duration.ofMillis

private val defaultStopMode = Graceful(ofMillis(1))
private fun HelidonWithGracefulShutdown(port: Int, ignored: StopMode) = Helidon(port, defaultStopMode)

class HelidonTest : ServerContract(::Helidon, ClientForServerTesting(), Method.entries.filterNot { it == Method.QUERY })
class HelidonGracefulTest : ServerContract(
    ::HelidonWithGracefulShutdown, ClientForServerTesting(),
    Method.entries.filterNot { it == Method.QUERY })
