package guide.howto.secure_and_auth_http

import org.http4k.client.OkHttp
import org.http4k.core.Body
import org.http4k.core.Credentials
import org.http4k.core.HttpHandler
import org.http4k.core.Method.GET
import org.http4k.core.Method.POST
import org.http4k.core.Request
import org.http4k.core.Response
import org.http4k.core.Status.Companion.OK
import org.http4k.core.Uri
import org.http4k.core.then
import org.http4k.core.with
import org.http4k.filter.ClientFilters
import org.http4k.filter.ServerFilters
import org.http4k.routing.bind
import org.http4k.routing.routes
import org.http4k.security.AccessTokenResponse
import org.http4k.security.CredentialsProvider
import org.http4k.security.ExpiringCredentials
import org.http4k.security.RefreshCredentials
import org.http4k.security.Refreshing
import org.http4k.security.oauth.client.OAuthClientCredentials
import org.http4k.security.oauth.client.RefreshingOAuthToken
import org.http4k.security.oauth.server.OAuthServerMoshi.auto
import org.http4k.server.Http4kServer
import org.http4k.server.SunHttp
import org.http4k.server.asServer
import java.time.Duration
import java.time.Instant

fun main() {
    val server = AuthServer().start()

    val baseHttp = ClientFilters.SetBaseUriFrom(Uri.of("http://localhost:${server.port()}"))
        .then(OkHttp())

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
        ExpiringCredentials(
            "bearerToken" + System.currentTimeMillis(),
            Instant.now().plusSeconds(5)
        )
    }

    val refreshingClient = ClientFilters.BearerAuth(
        CredentialsProvider.Refreshing(
            gracePeriod = Duration.ofSeconds(1),
            refreshFn = refreshFn
        )
    ).then(baseHttp)

    repeat(10) {
        println(refreshingClient(Request(GET, "/bearer")).bodyString())
        Thread.sleep(2000)
    }

    /**
     * auth against OAuth ClientCredentials flow using refreshing credentials
     */
    val clientCredentials = Credentials("id", "secret")

    val refreshingOAuthClient = ClientFilters.RefreshingOAuthToken(
        oauthCredentials = clientCredentials,
        tokenUri = Uri.of("/oauth"),
        scopes = emptyList(), // TODO include scope example
        backend = baseHttp,
        oAuthFlowFilter = ClientFilters.OAuthClientCredentials(clientCredentials),
        gracePeriod = Duration.ofSeconds(1)
    ).then(baseHttp)

    repeat(10) {
        println(refreshingOAuthClient(Request(GET, "/bearer")).bodyString())
        Thread.sleep(2000)
    }

    server.stop()
}

private fun AuthServer(): Http4kServer {
    val endpoint: HttpHandler = {
        Response(OK).body(it.header("Authorization").toString())
    }

    return routes(
        // statically check the user creds
        "basic" bind GET to
            ServerFilters.BasicAuth("realm", "username", "password").then(endpoint),
        // dynamically check the token
        "bearer" bind GET to
            ServerFilters.BearerAuth { it.startsWith("bearerToken") }.then(endpoint),
        // fake oauth token endpoint
        "oauth" bind POST to {
            println("refreshing oauth token (was " + it.bodyString() + ")")

            Response(OK).with(
                Body.auto<AccessTokenResponse>().toLens() of AccessTokenResponse(
                    access_token = "bearerTokenOAuth" + System.currentTimeMillis(),
                    expires_in = 5,
                    refresh_token = "refreshToken" + System.currentTimeMillis(),
                )
            )
        }
    ).asServer(SunHttp(8000))
}
