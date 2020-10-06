package guide.modules.servers

import org.http4k.core.Request
import org.http4k.core.Response
import org.http4k.core.Status.Companion.OK
import org.http4k.server.Jetty
import org.http4k.server.asServer

fun main() {
    { request: Request -> Response(OK).body("Hello World") }.asServer(Jetty(8000)).start()
}
