package guide.howto.lookup_a_user_principal

import org.http4k.core.Credentials
import org.http4k.core.Method.GET
import org.http4k.core.Request
import org.http4k.core.RequestContexts
import org.http4k.core.Response
import org.http4k.core.Status.Companion.OK
import org.http4k.core.then
import org.http4k.filter.ServerFilters.BearerAuth
import org.http4k.filter.ServerFilters.InitialiseRequestContext
import org.http4k.lens.RequestContextKey

fun main() {

    val contexts = RequestContexts()

    val credentials = RequestContextKey.required<Credentials>(contexts)

    val app = InitialiseRequestContext(contexts)
        .then(BearerAuth(credentials) { if (it == "42") Credentials("user", "pass") else null })
        .then { Response(OK).body(credentials(it).toString()) }

    println(app(Request(GET, "/").header("Authorization", "Bearer 41")))
    println(app(Request(GET, "/").header("Authorization", "Bearer 42")))
}
