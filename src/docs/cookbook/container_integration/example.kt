package cookbook.container_integration

import org.http4k.client.ApacheClient
import org.http4k.core.Method
import org.http4k.core.Request
import org.http4k.core.Response
import org.http4k.core.Status.Companion.OK
import org.http4k.server.Jetty
import org.http4k.server.startServer

/**
 * This example shows how to convert an HttpHandler to a server
 */
fun main(args: Array<String>) {

    val app = { request: Request -> Response(OK).body("Hello, ${request.query("name")}!") }

    val jettyServer = app.startServer(Jetty(9000), false)

    val request = Request(Method.GET, "http://localhost:9000").query("name", "John Doe")

    val client = ApacheClient()

    println(client(request))

    jettyServer.stop()
}
