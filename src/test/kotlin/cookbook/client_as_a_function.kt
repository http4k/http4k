package cookbook

import org.reekwest.http.client.ApacheHttpClient
import org.reekwest.http.core.Request.Companion.get

fun main(args: Array<String>) {

    val request = get("http://pokeapi.co/api/v2/pokemon/")

    val client = ApacheHttpClient()

    println(client(request))
}
