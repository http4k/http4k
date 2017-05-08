package cookbook

import org.http4k.http.core.HttpHandler
import org.http4k.http.core.Request
import org.http4k.http.core.Request.Companion.get
import org.http4k.http.core.Response.Companion.ok

fun main(args: Array<String>) {

    val app: HttpHandler = { request: Request -> ok().body("Hello, ${request.query("name")}!") }

    val request = get("/").query("name", "John Doe")

    val response = app(request)

    println(response)
}
