package guide.howto.secure_and_auth_http

import org.http4k.client.OkHttp
import org.http4k.core.HttpHandler
import org.http4k.core.Method.GET
import org.http4k.core.Request
import org.http4k.core.Response
import org.http4k.core.Status.Companion.OK
import org.http4k.core.Uri
import org.http4k.core.then
import org.http4k.filter.ClientFilters
import org.http4k.filter.ServerFilters
import org.http4k.routing.bind
import org.http4k.routing.routes
import org.http4k.security.CredentialsProvider
import org.http4k.security.ExpiringCredentials
import org.http4k.security.RefreshCredentials
import org.http4k.security.Refreshing
import org.http4k.server.Http4kServer
import org.http4k.server.SunHttp
import org.http4k.server.asServer
import java.time.Duration
import java.time.Instant

fun main() {
    val server = AuthServer().start()

    val baseHttp = ClientFilters.SetBaseUriFrom(Uri.of("http://localhost:${server.port()}")).then(OkHttp())

    /**
     * simplest hard coded basic auth details
     */
    println(
        ClientFilters.BasicAuth("username", "password")
            .then(baseHttp)(Request(GET, "/basic"))
    )

    /**
     * using a dynamically provided bearer token
     */
    println(
        ClientFilters.BearerAuth(CredentialsProvider { "bearerToken" + System.currentTimeMillis() })
            .then(baseHttp)(Request(GET, "/bearer"))
    )

    /**
     * using using auto-refreshed bearer token
     */

    // this is the refresh function - it is called when the old token is null or expired...
    val refreshFn = RefreshCredentials<String> { oldToken ->
        println("refreshing credentials (was $oldToken)")
        ExpiringCredentials("bearerToken" + System.currentTimeMillis(), Instant.now().plusSeconds(5))
    }

    val refreshingClient = ClientFilters.BearerAuth(
        CredentialsProvider.Refreshing(gracePeriod = Duration.ofSeconds(1), refreshFn = refreshFn)
    ).then(baseHttp)

    repeat(10) {
        println(refreshingClient(Request(GET, "/bearer")).bodyString())
        Thread.sleep(2000)
    }

    server.stop()
}

private fun AuthServer(): Http4kServer {
    val endpoint: HttpHandler = { Response(OK).body(it.uri.toString()) }

    return routes(
        "basic" bind GET to
            // statically check the user creds
            ServerFilters.BasicAuth("realm", "username", "password").then(endpoint),
        "bearer" bind GET to
            // dynamically check the token
            ServerFilters.BearerAuth { it.startsWith("bearerToken") }.then(endpoint)
    ).asServer(SunHttp(8000))
}

