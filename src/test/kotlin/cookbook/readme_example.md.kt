package cookbook

import org.http4k.client.OkHttp
import org.http4k.core.Filter
import org.http4k.core.HttpHandler
import org.http4k.core.Method.GET
import org.http4k.core.Request
import org.http4k.core.Response
import org.http4k.core.Status.Companion.OK
import org.http4k.core.then
import org.http4k.filter.CachingFilters
import org.http4k.routing.Route
import org.http4k.routing.RoutingHttpHandler
import org.http4k.routing.by
import org.http4k.routing.path
import org.http4k.routing.routes
import org.http4k.server.Jetty
import org.http4k.server.asServer

fun main(args: Array<String>) {
    // create an HttpHandler - which is just a function from  Request -> Response
    val friendlyHttpHandler: HttpHandler = { req: Request ->
        val path: String? = req.path("name")
        Response(OK).body("hello ${path ?: "anon!"}")
    }

    // we can bind HttpHandlers to paths/methods to create a Route
    val route: Route = "/greet/{name}" to GET by friendlyHttpHandler

    // combine many Routes together to make a RoutingHttpHandler (which is both a Router and an HttpHandler)
    val router: RoutingHttpHandler = routes(
        "/ping" to GET by { Response(OK).body("pong!") },
        route
    )

    // mount the Routers at separate contexts - to create another HttpHandler!
    val allRoutes: HttpHandler = routes(
        "/internal" by router,
        "/api" by router
    )

    // call the router in-memory without spinning up a server
    val inMemoryResponse: Response = allRoutes(Request(GET, "/api/greet/Bob"))
    println(inMemoryResponse)

// Produces:
//    HTTP/1.1 200 OK
//
//
//    hello Bob

    // this is a Filter - it performs pre/post processing on a request or response
    val timingFilter = Filter {
        next: HttpHandler ->
        {
            request: Request ->
            val start = System.currentTimeMillis()
            val response = next(request)
            val latency = System.currentTimeMillis() - start
            println("Request to ${request.uri} took ${latency}ms")
            response
        }
    }

    // we can "stack" filters to create reusable units, and then apply them to an HttpHandler
    val compositeFilter = CachingFilters.Response.NoCache().then(timingFilter)
    val app: HttpHandler = compositeFilter.then(allRoutes)

    // 1 LOC to mount an app and start it in a container
    app.asServer(Jetty(9000)).start()

    // HTTP clients are also HttpHandlers!
    val client: HttpHandler = OkHttp()

    val networkResponse: Response = client(Request(GET, "http://localhost:9000/api/greet/Bob"))
    println(networkResponse)

// Produces:
//    Request to /api/greet/Bob took 1ms
//    HTTP/1.1 200
//    cache-control: private, must-revalidate
//    content-length: 9
//    date: Thu, 08 Jun 2017 13:01:13 GMT
//    expires: 0
//    server: Jetty(9.3.16.v20170120)
//
//    hello Bob
}
