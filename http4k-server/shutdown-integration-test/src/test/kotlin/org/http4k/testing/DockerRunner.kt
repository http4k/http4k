package org.http4k.testing


fun main() {
    val server = ServerInDocker()
    val containerId = server.start()
    server.stop(containerId)
    println(server.eventsFor(containerId))
}

