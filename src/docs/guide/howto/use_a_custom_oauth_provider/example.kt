package guide.howto.use_a_custom_oauth_provider

import org.http4k.client.ApacheClient
import org.http4k.core.Credentials
import org.http4k.core.HttpHandler
import org.http4k.core.Method.GET
import org.http4k.core.Request
import org.http4k.core.Response
import org.http4k.core.Status.Companion.OK
import org.http4k.core.Uri
import org.http4k.core.then
import org.http4k.filter.ServerFilters
import org.http4k.routing.bind
import org.http4k.routing.routes
import org.http4k.security.AccessToken
import org.http4k.security.CrossSiteRequestForgeryToken
import org.http4k.security.Nonce
import org.http4k.security.OAuthPersistence
import org.http4k.security.OAuthProvider
import org.http4k.security.OAuthProviderConfig
import org.http4k.security.openid.IdToken
import org.http4k.server.SunHttp
import org.http4k.server.asServer

// this example shows how to configure a custom provider for the OAuth2 Auth Code Grant flow
fun main() {
    val port = 9000

    // the callback uri which is configured in our OAuth provider
    val callbackUri = Uri.of("http://localhost:$port/callback")

    // custom OAuth2 provider configuration
    val oauthProvider = OAuthProvider(
        OAuthProviderConfig(
            authBase = Uri.of("https://auth.chatroulette.com"),
            authPath = "/oauth2/auth", tokenPath = "/oauth2/token",
            credentials = Credentials("username", "somepassword"),
            apiBase = Uri.of("https://api.chatroulette.com")
        ),
        ApacheClient(),
        callbackUri,
        listOf("emailScope", "nameScope", "familyScope"),
        CustomOAuthPersistence()
    )

    val app: HttpHandler =
        routes(
            callbackUri.path bind GET to oauthProvider.callback,
            "/" bind GET to oauthProvider.authFilter.then { Response(OK).body("hello!") }
        )

    ServerFilters.CatchAll()
        .then(app)
        .asServer(SunHttp(port)).start().block()
}

// this interface allows us to provide custom logic for storing and verifying the CSRF and AccessTokens.
// to be maximally secure, never let the end-user see the access token!
// also avoid allowing third parties set the original uri as this might allow phishing attacks
// on strategy is might be to use an enum to map to a set of know uris
// e.g. shoppingCart -> /cart
class CustomOAuthPersistence : OAuthPersistence {
    var nonce: Nonce? = null
    var csrf: CrossSiteRequestForgeryToken? = null
    var accessToken: AccessToken? = null
    var originalUri: Uri? = null

    override fun retrieveCsrf(request: Request): CrossSiteRequestForgeryToken? = csrf

    override fun assignCsrf(redirect: Response, csrf: CrossSiteRequestForgeryToken): Response {
        this.csrf = csrf
        return redirect.header("action", "assignCsrf")
    }

    override fun assignNonce(redirect: Response, nonce: Nonce): Response {
        this.nonce = nonce
        return redirect.header("action", "assignNonce")
    }

    override fun retrieveNonce(request: Request): Nonce? = nonce

    override fun assignOriginalUri(redirect: Response, originalUri: Uri): Response {
        this.originalUri = originalUri
        return redirect.header("action", "assignOriginalUri")
    }

    override fun retrieveOriginalUri(request: Request): Uri? = originalUri

    override fun retrieveToken(request: Request): AccessToken? = accessToken

    override fun assignToken(
        request: Request,
        redirect: Response,
        accessToken: AccessToken,
        idToken: IdToken?
    ): Response {
        this.accessToken = accessToken
        return redirect.header("action", "assignToken")
    }
}
