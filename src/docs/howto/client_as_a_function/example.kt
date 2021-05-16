package howto.client_as_a_function

import org.http4k.client.JavaHttpClient
import org.http4k.core.HttpHandler
import org.http4k.core.Method
import org.http4k.core.Request

fun main() {

    val request = Request(Method.GET, "https://xkcd.com/info.0.json")

    val client: HttpHandler = JavaHttpClient()

    println(client(request))
}
