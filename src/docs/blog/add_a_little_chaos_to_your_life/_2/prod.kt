package blog.add_a_little_chaos_to_your_life._2

import org.http4k.core.HttpHandler
import org.http4k.core.Method.GET
import org.http4k.core.Request
import org.http4k.core.Response
import org.http4k.core.Status.Companion.OK
import org.http4k.core.then
import org.http4k.filter.ClientFilters
import org.http4k.filter.HandleRemoteRequestFailed
import org.http4k.filter.ServerFilters
import org.http4k.routing.RoutingHttpHandler
import org.http4k.routing.bind
import org.http4k.routing.routes

class Library(rawHttp: HttpHandler) {
    private val http = ClientFilters.HandleRemoteRequestFailed().then(rawHttp)

    fun titles(): List<String> =
        http(Request(GET, "/titles")).bodyString().split(",").map { it.trim() }.sorted()
}

fun Server(http: HttpHandler): RoutingHttpHandler {
    val library = Library(http)

    return ServerFilters.HandleRemoteRequestFailed()
        .then(
            routes("/reference/api/books" bind GET to {
                Response(OK).body(library.titles().joinToString(","))
            })
        )
}
