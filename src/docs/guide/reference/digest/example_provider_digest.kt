package guide.reference.digest

import org.http4k.core.Method.GET
import org.http4k.core.Response
import org.http4k.core.Status.Companion.OK
import org.http4k.core.then
import org.http4k.filter.DigestAuth
import org.http4k.filter.ServerFilters
import org.http4k.routing.bind
import org.http4k.routing.path
import org.http4k.routing.routes
import org.http4k.server.SunHttp
import org.http4k.server.asServer

fun main() {
    val users = mapOf(
        "admin" to "password",
        "user" to "hunter2"
    )

    val routes = routes(
        "/hello/{name}" bind GET to { request ->
            val name = request.path("name")
            Response(OK).body("Hello $name")
        }
    )

    val authFilter = ServerFilters.DigestAuth(
        realm = "http4k",
        passwordLookup = { username -> users[username] })

    authFilter
        .then(routes)
        .asServer(SunHttp(8000))
        .start()
        .block()
}
