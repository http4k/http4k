package cookbook

import org.http4k.client.ApacheHttpClient
import org.http4k.http.core.Request.Companion.get

fun main(args: Array<String>) {

    val request = get("http://pokeapi.co/api/v2/pokemon/")

    val client = ApacheHttpClient()

    println(client(request))
}
