package site.cookbook

import org.http4k.client.ApacheClient
import org.http4k.core.HttpHandler
import org.http4k.core.Method
import org.http4k.core.Request

/**
 * This example demonstrates a client module (in this case the Apache Client). A client is just another
 * HttpHandler.
 */
fun main(args: Array<String>) {

    val request = Request(Method.GET, "http://pokeapi.co/api/v2/pokemon/")

    val client: HttpHandler = ApacheClient()

    println(client(request))
}
