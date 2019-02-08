package cookbook.readme

import org.http4k.client.OkHttp
import org.http4k.core.Filter
import org.http4k.core.HttpHandler
import org.http4k.core.Method.GET
import org.http4k.core.Request
import org.http4k.core.Response
import org.http4k.core.Status.Companion.OK
import org.http4k.core.then
import org.http4k.filter.CachingFilters
import org.http4k.routing.bind
import org.http4k.routing.path
import org.http4k.routing.routes
import org.http4k.server.Jetty
import org.http4k.server.asServer

fun main() {
    // we can bind HttpHandlers (which are just functions from  Request -> Response) to paths/methods to create a Route,
    // then combine many Routes together to make another HttpHandler
    val app: HttpHandler = routes(
        "/ping" bind GET to { Response(OK).body("pong!") },
        "/greet/{name}" bind GET to { req: Request ->
            val path: String = req.path("name")!!
            Response(OK).body("hello $path")
        }
    )

    // call the handler in-memory without spinning up a server
    val inMemoryResponse: Response = app(Request(GET, "/greet/Bob"))
    println(inMemoryResponse)

// Produces:
//    HTTP/1.1 200 OK
//
//
//    hello Bob

    // this is a Filter - it performs pre/post processing on a request or response
    val timingFilter = Filter { next ->
        { request: Request ->
            val start = System.currentTimeMillis()
            val response = next(request)
            val latency = System.currentTimeMillis() - start
            println("Request to ${request.uri} took ${latency}ms")
            response
        }
    }

    // we can "stack" filters to create reusable units, and then apply them to an HttpHandler
    val compositeFilter = CachingFilters.Response.NoCache().then(timingFilter)
    val filteredApp: HttpHandler = compositeFilter.then(app)

    // only 1 LOC to mount an app and start it in a container
    filteredApp.asServer(Jetty(9000)).start()

    // HTTP clients are also HttpHandlers!
    val client: HttpHandler = OkHttp()

    val networkResponse: Response = client(Request(GET, "http://localhost:9000/greet/Bob"))
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
