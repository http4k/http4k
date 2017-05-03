package cookbook

import org.reekwest.http.apache.ApacheHttpClient
import org.reekwest.http.core.Request
import org.reekwest.http.core.Request.Companion.get
import org.reekwest.http.core.Response.Companion.ok
import org.reekwest.http.core.bodyString
import org.reekwest.http.jetty.asJettyServer

fun main(args: Array<String>) {

    val app = { request: Request -> ok().bodyString("Hello, ${request.query("name")}!") }

    val jettyServer = app.asJettyServer(9000)

    jettyServer.start()

    val request = get("http://localhost:9000").query("name", "John Doe")

    val client = ApacheHttpClient()

    println(client(request))

    jettyServer.stop()
}
