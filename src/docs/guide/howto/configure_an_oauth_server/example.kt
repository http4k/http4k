import dev.forkhandles.result4k.Failure
import dev.forkhandles.result4k.Success
import org.http4k.client.OkHttp
import org.http4k.core.Credentials
import org.http4k.core.HttpHandler
import org.http4k.core.Method.GET
import org.http4k.core.Method.POST
import org.http4k.core.Request
import org.http4k.core.Response
import org.http4k.core.Status.Companion.OK
import org.http4k.core.Uri
import org.http4k.core.then
import org.http4k.format.Jackson
import org.http4k.routing.RoutingHttpHandler
import org.http4k.routing.bind
import org.http4k.routing.routes
import org.http4k.security.AccessToken
import org.http4k.security.InsecureCookieBasedOAuthPersistence
import org.http4k.security.OAuthProvider
import org.http4k.security.OAuthProviderConfig
import org.http4k.security.oauth.server.AccessTokens
import org.http4k.security.oauth.server.AuthRequest
import org.http4k.security.oauth.server.AuthorizationCode
import org.http4k.security.oauth.server.AuthorizationCodeDetails
import org.http4k.security.oauth.server.AuthorizationCodes
import org.http4k.security.oauth.server.ClientId
import org.http4k.security.oauth.server.ClientValidator
import org.http4k.security.oauth.server.InsecureCookieBasedAuthRequestTracking
import org.http4k.security.oauth.server.OAuthServer
import org.http4k.security.oauth.server.TokenRequest
import org.http4k.security.oauth.server.UnsupportedGrantType
import org.http4k.security.oauth.server.accesstoken.AuthorizationCodeAccessTokenRequest
import org.http4k.server.Jetty
import org.http4k.server.asServer
import java.time.Clock
import java.time.temporal.ChronoUnit.DAYS
import java.util.UUID

fun main() {
    fun authorizationServer(): RoutingHttpHandler {
        val server = OAuthServer(
            tokenPath = "/oauth2/token",
            authRequestTracking = InsecureCookieBasedAuthRequestTracking(),
            clientValidator = InsecureClientValidator(),
            authorizationCodes = InsecureAuthorizationCodes(),
            accessTokens = InsecureAccessTokens(),
            json = Jackson,
            clock = Clock.systemUTC(),
            documentationUri = "See the full API docs at https://example.com/docs/access_token"
        )

        return routes(
            server.tokenRoute,
            "/my-login-page" bind GET to server.authenticationStart.then {
                Response(OK).body(
                    """
                    <html>
                        <form method="POST">
                            <button type="submit">Please authenticate</button>
                        </form>
                    </html>
                    """.trimIndent()
                )
            },
            "/my-login-page" bind POST to server.authenticationComplete
        )
    }

    fun oAuthClientApp(tokenClient: HttpHandler): RoutingHttpHandler {
        val persistence = InsecureCookieBasedOAuthPersistence("oauthTest")
        val authorizationServer = Uri.of("http://localhost:9000")

        val oauthProvider = OAuthProvider(
            OAuthProviderConfig(
                authorizationServer,
                "/my-login-page", "/oauth2/token",
                Credentials("my-app", "somepassword")
            ),
            tokenClient,
            Uri.of("http://localhost:8000/my-callback"),
            listOf("name", "age"),
            persistence
        )

        return routes(
            "/my-callback" bind GET to oauthProvider.callback,
            "/a-protected-resource" bind GET to oauthProvider.authFilter.then {
                Response(OK).body(
                    "user's protected resource"
                )
            }
        )
    }

    oAuthClientApp(OkHttp()).asServer(Jetty(8000)).start()
    authorizationServer().asServer(Jetty(9000)).start().block()

    // Go to http://localhost:8000/a-protected-resource to start the authorization flow
}

// This class allow you to make extra checks about the oauth client during the flow
class InsecureClientValidator : ClientValidator {
    // the client id should be a registered one
    override fun validateClientId(request: Request, clientId: ClientId): Boolean = true

    // one should only redirect to URLs registered against a particular client
    override fun validateRedirection(
        request: Request,
        clientId: ClientId,
        redirectionUri: Uri
    ): Boolean = true

    // one should validate the scopes are correct for that client
    override fun validateScopes(
        request: Request,
        clientId: ClientId,
        scopes: List<String>
    ): Boolean = true

    // certain operations can only be performed by fully authenticated clients
    // e.g. generate access tokens
    override fun validateCredentials(
        request: Request,
        clientId: ClientId,
        clientSecret: String
    ): Boolean = true
}

class InsecureAuthorizationCodes : AuthorizationCodes {
    private val clock = Clock.systemUTC()
    private val codes = mutableMapOf<AuthorizationCode, AuthorizationCodeDetails>()

    override fun detailsFor(code: AuthorizationCode) =
        codes[code] ?: error("code not stored")

    // Authorization codes should be associated
    // to a particular user (who can be identified in the Response)
    // so they can be checked in various stages of the authorization flow
    override fun create(request: Request, authRequest: AuthRequest, response: Response) =
        Success(AuthorizationCode(UUID.randomUUID().toString()).also {
            codes[it] = AuthorizationCodeDetails(
                authRequest.client,
                authRequest.redirectUri!!,
                clock.instant().plus(1, DAYS),
                authRequest.state,
                authRequest.isOIDC()
            )
        })
}

class InsecureAccessTokens : AccessTokens {
    override fun create(
        clientId: ClientId,
        tokenRequest: TokenRequest
    ) = Failure(UnsupportedGrantType("client_credentials"))

    // an access token should be associated with a particular authorization flow
    // (i.e. limited to the requested scopes), and contain an expiration date
    override fun create(
        clientId: ClientId,
        tokenRequest: AuthorizationCodeAccessTokenRequest
    ) = Success(AccessToken(UUID.randomUUID().toString()))
}
