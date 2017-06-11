package cookbook.client_as_a_function

import org.http4k.client.ApacheClient
import org.http4k.core.HttpHandler
import org.http4k.core.Method
import org.http4k.core.Request

fun main(args: Array<String>) {

    val request = Request(Method.GET, "http://pokeapi.co/api/v2/pokemon/")

    val client: HttpHandler = ApacheClient()

    println(client(request))
}
