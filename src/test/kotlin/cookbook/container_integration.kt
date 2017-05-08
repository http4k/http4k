package cookbook

import org.http4k.client.ApacheHttpClient
import org.http4k.core.Request
import org.http4k.core.Request.Companion.get
import org.http4k.core.Response.Companion.ok
import org.http4k.server.asJettyServer

fun main(args: Array<String>) {

    val app = { request: Request -> ok().body("Hello, ${request.query("name")}!") }

    val jettyServer = app.asJettyServer(9000)

    jettyServer.start()

    val request = get("http://localhost:9000").query("name", "John Doe")

    val client = ApacheHttpClient()

    println(client(request))

    jettyServer.stop()
}
