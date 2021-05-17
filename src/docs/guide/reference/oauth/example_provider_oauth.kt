package guide.reference.oauth

import org.http4k.client.ApacheClient
import org.http4k.core.Credentials
import org.http4k.core.HttpHandler
import org.http4k.core.Method.GET
import org.http4k.core.Response
import org.http4k.core.Status.Companion.OK
import org.http4k.core.Uri
import org.http4k.core.then
import org.http4k.filter.ServerFilters
import org.http4k.routing.bind
import org.http4k.routing.routes
import org.http4k.security.InsecureCookieBasedOAuthPersistence
import org.http4k.security.OAuthProvider
import org.http4k.security.google
import org.http4k.server.SunHttp
import org.http4k.server.asServer

fun main() {

    // set these before running this example
    val googleClientId = System.getenv("CLIENT_ID")
    val googleClientSecret = System.getenv("CLIENT_SECRET")

    val port = 9000

    // the callback uri which is configured in our OAuth provider
    val callbackUri = Uri.of("http://localhost:$port/callback")

    // this is a test implementation of the OAuthPersistence interface, which should be
    // implemented by application developers
    val oAuthPersistence = InsecureCookieBasedOAuthPersistence("Google")

    // pre-defined configuration exist for common OAuth providers
    val oauthProvider = OAuthProvider.google(
        ApacheClient(),
        Credentials(googleClientId, googleClientSecret),
        callbackUri,
        oAuthPersistence
    )

    // the 2 main points here are the callback handler and the authFilter, which protects the root resource
    val app: HttpHandler =
        routes(
            callbackUri.path bind GET to oauthProvider.callback,
            "/" bind GET to oauthProvider.authFilter.then { Response(OK).body("hello!") }
        )

    ServerFilters.CatchAll()
        .then(app)
        .asServer(SunHttp(port)).start().block()
}

// browse to: http://localhost:9000 - you'll be redirected to google for authentication
