package guide.modules.serverless

import org.http4k.core.Filter
import org.http4k.core.HttpHandler
import org.http4k.core.Method
import org.http4k.core.Request
import org.http4k.core.Response
import org.http4k.core.Status.Companion.INTERNAL_SERVER_ERROR
import org.http4k.core.Status.Companion.OK
import org.http4k.core.then
import org.http4k.routing.bind
import org.http4k.routing.routes
import org.http4k.serverless.gcf.GoogleCloudFunction

val routes = routes(
    "/success" bind Method.GET to { request: Request -> Response(OK).body("works!") },
    "/fail" bind Method.GET to { request: Request -> Response(INTERNAL_SERVER_ERROR) }
)
val timer = Filter { next: HttpHandler ->
    { request: Request ->
        val start = System.currentTimeMillis()
        val response = next(request)
        val latency = System.currentTimeMillis() - start
        println("I took $latency ms")
        response
    }
}

class FunctionsExampleEntryClass : GoogleCloudFunction(timer.then(routes))
