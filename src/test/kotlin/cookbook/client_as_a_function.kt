package cookbook

import org.http4k.client.ApacheClient
import org.http4k.core.Method
import org.http4k.core.Request

fun main(args: Array<String>) {

    val request = Request(Method.GET, "http://pokeapi.co/api/v2/pokemon/")

    val client = ApacheClient()

    println(client(request))
}
