package org.http4k.testing


fun main() {
    val server = ServerInDocker()
    val containerId = server.start()
    println(server.eventsFor(containerId))
}

