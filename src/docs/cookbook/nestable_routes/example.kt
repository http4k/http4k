package cookbook.nestable_routes

import org.http4k.core.Method.DELETE
import org.http4k.core.Method.GET
import org.http4k.core.Method.POST
import org.http4k.core.Request
import org.http4k.core.Response
import org.http4k.core.Status.Companion.OK
import org.http4k.core.then
import org.http4k.filter.DebuggingFilters.PrintRequestAndResponse
import org.http4k.routing.ResourceLoader.Companion.Classpath
import org.http4k.routing.and
import org.http4k.routing.bind
import org.http4k.routing.headers
import org.http4k.routing.path
import org.http4k.routing.queries
import org.http4k.routing.routes
import org.http4k.routing.singlePageApp
import org.http4k.routing.static

fun main() {
    val routesWithFilter =
        PrintRequestAndResponse().then(
            routes(
                "/get/{name}" bind GET to { req: Request -> Response(OK).body(req.path("name")!!) },
                "/post/{name}" bind POST to { _: Request -> Response(OK) }
            )
        )
    println(routesWithFilter(Request(GET, "/get/value")))

    val staticWithFilter = PrintRequestAndResponse().then(static(Classpath("cookbook/nestable_routes")))
    val app = routes(
        "/bob" bind routesWithFilter,
        "/static" bind staticWithFilter,
        "/pattern/{rest:.*}" bind { req: Request -> Response(OK).body(req.path("rest") ?: "") },
        "/rita" bind routes(
            "/delete/{name}" bind DELETE to { _: Request -> Response(OK) },
            "/post/{name}" bind POST to { _: Request -> Response(OK) }
        ),
        "/matching" bind GET to routes(
            headers("requiredheader").and(queries("requiredquery")) bind { Response(OK).body("matched 2 parameters") },
            headers("requiredheader") bind { Response(OK).body("matched 1 parameters") }
        ),
        singlePageApp(Classpath("cookbook/nestable_routes"))
    )

    println(app(Request(GET, "/bob/get/value")))
    println(app(Request(GET, "/static/someStaticFile.txt")))
    println(app(Request(GET, "/pattern/some/entire/pattern/we/want/to/capture")))
    println(app(Request(GET, "/matching").header("requiredheader", "somevalue").query("requiredquery", "somevalue")))
    println(app(Request(GET, "/matching").header("requiredheader", "somevalue")))
    println(app(Request(GET, "/someSpaResource")))
}
