package cookbook

import org.reekwest.http.core.HttpHandler
import org.reekwest.http.core.Request
import org.reekwest.http.core.Request.Companion.get
import org.reekwest.http.core.Response.Companion.ok
import org.reekwest.http.core.bodyString

fun main(args: Array<String>) {

    val app: HttpHandler = { request: Request -> ok().bodyString("Hello, ${request.query("name")}!") }

    val request = get("/").query("name", "John Doe")

    val response = app(request)

    println(response)
}
