package cookbook

import org.http4k.core.Method.DELETE
import org.http4k.core.Method.GET
import org.http4k.core.Method.POST
import org.http4k.core.Request
import org.http4k.core.Response
import org.http4k.core.Status.Companion.OK
import org.http4k.core.then
import org.http4k.filter.DebuggingFilters.PrintRequestAndResponse
import org.http4k.routing.ResourceLoader.Companion.Classpath
import org.http4k.routing.by
import org.http4k.routing.path
import org.http4k.routing.routes
import org.http4k.routing.static

fun main(args: Array<String>) {
    val routesWithFilter =
        PrintRequestAndResponse().then(
            routes(
                GET to "/get/{name}" by { req: Request -> Response(OK).body(req.path("name")!!) },
                POST to "/post/{name}" by { _: Request -> Response(OK) }
            )
        )
    println(routesWithFilter(Request(GET, "/get/value")))

    val staticWithFilter = PrintRequestAndResponse().then(static(Classpath("cookbook")))
    val app = routes(
        "/bob" by routesWithFilter,
        "/static" by staticWithFilter,
        "/rita" by routes(
            DELETE to "/delete/{name}" by { _: Request -> Response(OK) },
            POST to "/post/{name}" by { _: Request -> Response(OK) }
        )
    )

    println(app(Request(GET, "/bob/get/value")))
    println(app(Request(GET, "/static/someStaticFile.txt")))
}
