package blog.add_a_little_chaos_to_your_life._1

import org.http4k.core.HttpHandler
import org.http4k.core.Method.GET
import org.http4k.core.Request
import org.http4k.core.Response
import org.http4k.core.Status.Companion.OK
import org.http4k.core.Status.Companion.SERVICE_UNAVAILABLE
import org.http4k.routing.RoutingHttpHandler
import org.http4k.routing.bind
import org.http4k.routing.routes

class Library(private val rawHttp: HttpHandler) {
    fun titles(): List<String> {
        val response = rawHttp(Request(GET, "/titles"))
        return when (OK) {
            response.status -> response.bodyString().split(",").map { it.trim() }.sorted()
            else -> throw Exception("Bad times")
        }
    }
}

fun Server(http: HttpHandler): RoutingHttpHandler {
    val library = Library(http)

    return routes("/reference/api/books" bind GET to {
        try {
            Response(OK).body(library.titles().joinToString(","))
        } catch (e: Exception) {
            Response(SERVICE_UNAVAILABLE)
        }
    })
}
