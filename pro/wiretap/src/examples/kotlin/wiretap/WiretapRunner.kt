package wiretap

import org.http4k.server.Jetty
import org.http4k.server.asServer
import org.http4k.server.uri

fun main() {
    val wiretap = WiretapExamples.httpAppWithOtelTracing()

    val server = wiretap.asServer(Jetty(21000)).start()
    println("started ${server.uri().path("_wiretap")}")
}


