package site.cookbook

import org.http4k.core.HttpHandler
import org.http4k.core.Method
import org.http4k.core.Request
import org.http4k.core.Response
import org.http4k.core.Status

/**
 * This is the simplest example of "Server as a function". There is no routing involved, nor any server. Testing
 * can be done out-of-container.
 */
fun main(args: Array<String>) {

    val app: HttpHandler = { request: Request -> Response(Status.OK).body("Hello, ${request.query("name")}!") }

    val request = Request(Method.GET, "/").query("name", "John Doe")

    val response = app(request)

    println(response)
}
