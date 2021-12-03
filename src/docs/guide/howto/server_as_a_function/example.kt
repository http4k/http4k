package guide.howto.server_as_a_function

import org.http4k.core.HttpHandler
import org.http4k.core.Method
import org.http4k.core.Request
import org.http4k.core.Response
import org.http4k.core.Status

fun main() {

    val app: HttpHandler = { request: Request -> Response(Status.OK).body("Hello, ${request.query("name")}!") }

    val request = Request(Method.GET, "/").query("name", "John Doe")

    val response = app(request)

    println(response)
}
