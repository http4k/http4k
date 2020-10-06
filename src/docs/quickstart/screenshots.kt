package quickstart

import org.http4k.client.ApacheClient
import org.http4k.core.Method.GET
import org.http4k.core.Request
import org.http4k.core.Response
import org.http4k.core.Status.Companion.OK
import org.http4k.server.Jetty
import org.http4k.server.asServer

fun main() {

    val app = { request: Request ->
        Response(OK)
            .body("Hello, ${request.query("name")}!")
    }

    app.asServer(Jetty(9000)).start()

    val client = ApacheClient()

    val request = Request(GET, "http://localhost:9000")
        .query("name", "John Doe")

    val response: Response = client(request)

    println(response)
}
