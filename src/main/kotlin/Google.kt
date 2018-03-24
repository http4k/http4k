import org.http4k.core.Credentials
import org.http4k.core.Filter
import org.http4k.core.HttpHandler
import org.http4k.core.Method.POST
import org.http4k.core.Request
import org.http4k.core.Response
import org.http4k.core.Status.Companion.FORBIDDEN
import org.http4k.core.Status.Companion.TEMPORARY_REDIRECT
import org.http4k.core.Uri
import org.http4k.core.body.form
import org.http4k.core.cookie.Cookie
import org.http4k.core.cookie.cookie
import org.http4k.core.cookie.invalidateCookie
import org.http4k.core.query
import org.http4k.core.then
import org.http4k.core.with
import org.http4k.filter.ClientFilters
import org.http4k.lens.Header.Common.LOCATION
import org.http4k.lens.Query
import org.http4k.lens.uri
import java.math.BigInteger
import java.security.SecureRandom
import java.util.UUID

/**
 *     https://accounts.google.com/o/oauth2/v2/auth?
client_id=424911365001.apps.googleusercontent.com&
response_type=code&
scope=openid%20email&
redirect_uri=http://localhost:9000/callback
state=below
encode!!    security_token=138r5719ru3e1&url=https://oauth2-login-demo.example.com/myHome&
nonce=random&
 */

data class AntiForgeryStateToken(val state: String) {
    companion object {
        fun secure() = AntiForgeryStateToken(BigInteger(130, SecureRandom()).toString(32))
    }
}

data class OAuthClientConfig(
    val serviceName: String,
    private val authBase: Uri,
    private val authPath: String,
    val apiBase: Uri,
    val tokenPath: String,
    val credentials: Credentials,
    val callbackUri: Uri,
    val scopes: List<String>) {
    val authUri = authBase.path(authPath)

    companion object
}

internal object GoogleApi {
    val redirectUri = Query.uri().required("redirect_uri")
}


/**
 * POST www.googleapis.com/oauth2/v4/token
Content-Type: application/x-www-form-urlencoded
code=4/P7q7W91a-oMsCeLvIaQm6bTrgtp7&
client_id=8819981768.apps.googleusercontent.com&
client_secret={client_secret}&
redirect_uri=https://oauth2-login-demo.example.com/code&
grant_type=authorization_code
 */

class OAuth(client: HttpHandler,
            private val oAuthClientConfig: OAuthClientConfig,
            private val generateCrsf: () -> String = { BigInteger(130, SecureRandom()).toString(32) },
            private val generateNonce: () -> String = { UUID.randomUUID().toString() }) {

    private val crsfCookieName = "csrf"
    private val tokenCookieName = "accessToken"

    val api = ClientFilters.SetHostFrom(oAuthClientConfig.apiBase).then(client)

    val authFilter = object : Filter {
        override fun invoke(next: HttpHandler): HttpHandler {
            return { req ->
                req.cookie(tokenCookieName)?.let { next(req) } ?: redirectToAuth(req.uri)
            }
        }
    }

    val callback: HttpHandler = {
        //http://localhost:9000/callback?state=/?csrf%3Dehnst6i3n89khhn0v27ks6t8gn&code=4/AABog4Jo0BMKTmTKZR87LnGES1U4Q2deF6MbBMvqs4fiDKhTZr0LT0GPYAZ-mBX2gO8JwJZVlvjQ9k_D50K1MIg&authuser=0&session_state=7c0f1ea0f7dff7252ed75788864337e165d75232..05be&prompt=consent#
        it.query("code")
            ?.let { code -> it.cookie(crsfCookieName)?.let { code to it } }
            ?.let { (code, csrfCookie) ->
                println("hit callback$ $code, $csrfCookie")
                when {
                    csrfCookie.value != it.query("csrf") -> Response(FORBIDDEN).invalidateCookie(crsfCookieName)
                    else -> codeToAccessToken(code, Uri.of(""))
                }
            } ?: Response(FORBIDDEN)
    }

    private fun codeToAccessToken(code: String, originalUri: Uri): Response {
        val accessToken = api(Request(POST, oAuthClientConfig.tokenPath)
            .form("grant_type", "authorization_code")
            .form("redirect_uri", originalUri.toString())
            .form("client_id", oAuthClientConfig.credentials.user)
            .form("client_secret", oAuthClientConfig.credentials.password)
            .form("code", code))

        println(accessToken.bodyString())
        return Response(TEMPORARY_REDIRECT).cookie(Cookie(tokenCookieName, accessToken.bodyString()))
    }

    private fun redirectToAuth(originalUri: Uri) = generateCrsf().let {
        Response(TEMPORARY_REDIRECT).with(LOCATION of oAuthClientConfig.authUri
            .query("client_id", oAuthClientConfig.credentials.user)
            .query("response_type", "code")
            .query("scope", oAuthClientConfig.scopes.joinToString(" "))
            .query("redirect_uri", oAuthClientConfig.callbackUri.toString())
            .query("state", originalUri.query(crsfCookieName, it).toString())
            .query("nonce", generateNonce()))
            .cookie(Cookie(crsfCookieName, it))
    }

    companion object
}

fun OAuth.Companion.google(client: HttpHandler, credentials: Credentials, callbackUri: Uri) =
    OAuth(
        client,
        OAuthClientConfig("Google",
            Uri.of("https://accounts.google.com"),
            "/o/oauth2/v2/auth",
            Uri.of("https://www.googleapis.com"),
            "/oauth2/v4/token",
            credentials,
            callbackUri,
            listOf("openid", "email"))

    )