package org.http4k.testing

import org.http4k.server.ServerConfig.StopMode.Graceful
import java.time.Duration


fun main() {
    val server = ServerInDocker()
    val containerId = server.start(ServerBackend.Apache, Graceful(Duration.ofSeconds(5)))
    server.stop(containerId)
    println(server.eventsFor(containerId))
}

