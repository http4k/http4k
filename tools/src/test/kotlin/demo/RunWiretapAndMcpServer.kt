package demo

import org.http4k.core.HttpHandler
import org.http4k.filter.debugMcp
import org.http4k.server.Jetty
import org.http4k.server.asServer
import org.http4k.server.uri
import org.http4k.wiretap.LocalTarget
import org.http4k.wiretap.Wiretap

fun main() {
    val eventsClient: HttpHandler = EventsServer()

    val target = LocalTarget.poly { McpDemo(http(eventsClient)).debugMcp() }

    val server = Wiretap(target).asServer(Jetty(12345)).start()

    println(server.uri().path("_wiretap"))
}



