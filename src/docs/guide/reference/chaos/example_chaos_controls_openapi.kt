package guide.reference.chaos

import org.http4k.chaos.withChaosApi
import org.http4k.core.Response
import org.http4k.core.Status.Companion.OK
import org.http4k.core.then
import org.http4k.filter.CorsPolicy.Companion.UnsafeGlobalPermissive
import org.http4k.filter.ServerFilters
import org.http4k.filter.ServerFilters.Cors
import org.http4k.server.SunHttp
import org.http4k.server.asServer

fun main() {
    Cors(UnsafeGlobalPermissive)
        .then(ServerFilters.CatchAll())
        .then { Response(OK).body("A normal response") }
        .withChaosApi()
        .asServer(SunHttp(9000))
        .start()
        .also { println("Visit the app at http://localhost:9000 or see the OpenApi at https://www.http4k.org/openapi3/?url=http://localhost:9000/chaos") }
}
