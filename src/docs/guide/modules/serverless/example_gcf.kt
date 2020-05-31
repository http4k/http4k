package guide.modules.serverless

import org.http4k.core.*
import org.http4k.core.Status.Companion.INTERNAL_SERVER_ERROR
import org.http4k.core.Status.Companion.OK
import org.http4k.routing.bind
import org.http4k.routing.routes
import org.http4k.serverless.gcp.Http4kGCFAdapter

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

class FunctionsExampleEntryClass : Http4kGCFAdapter(timer.then(routes))
