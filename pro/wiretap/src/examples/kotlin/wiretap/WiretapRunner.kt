package wiretap

import org.http4k.client.JavaHttpClient
import org.http4k.core.Method.*
import org.http4k.core.Request
import org.http4k.server.Jetty
import org.http4k.server.asServer
import org.http4k.server.uri

fun main() {
    val wiretap = HttpApp()
//    val wiretap = HttpAppWithOtelTracing()
//    val wiretap = McpApp()
//    val wiretap = McpServer()
//    val wiretap = McpServerWithOtel()
//    val wiretap = OpenApiApp()
//    val wiretap = ExternalMcpServer()
//    val wiretap = ExternalMcpApp()
//    val wiretap = ExternalWebsite()

    val server = wiretap.asServer(Jetty(21000)).start()

    println("started ${server.uri().path("_wiretap")}")

    (0..5).forEach {
        JavaHttpClient()(Request(GET, server.uri().path(it.toString())))
    }

}


