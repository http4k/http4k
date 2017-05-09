package cookbook

import org.http4k.client.ApacheClient
import org.http4k.core.Request.Companion.get

fun main(args: Array<String>) {

    val request = get("http://pokeapi.co/api/v2/pokemon/")

    val client = ApacheClient()

    println(client(request))
}
