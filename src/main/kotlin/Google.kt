
import GoogleApi.clientId
import GoogleApi.nonce
import GoogleApi.redirectUri
import GoogleApi.responseType
import GoogleApi.scope
import GoogleApi.state
import org.http4k.core.Credentials
import org.http4k.core.Filter
import org.http4k.core.HttpHandler
import org.http4k.core.Response
import org.http4k.core.Status.Companion.TEMPORARY_REDIRECT
import org.http4k.core.Uri
import org.http4k.core.cookie.Cookie
import org.http4k.core.cookie.cookie
import org.http4k.core.query
import org.http4k.core.with
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

data class GoogleAuthorizeRequest(
    val client: Credentials,
    val antiForgeryStateToken: AntiForgeryStateToken,
    val callbackUri: Uri,
    val originalUri: Uri,
    val nonce: String,
    val scopes: List<String>
)

internal object GoogleApi {
    val clientId = Query.required("client_id")
    val responseType = Query.required("response_type")
    val scope = Query.map { it.split(" ") }.required("scope")
    val redirectUri = Query.uri().required("redirect_uri")
    val state = Query.map(::AntiForgeryStateToken).required("state")
    val nonce = Query.required("nonce")
    val code = Query.required("code")
}

fun Uri.with(req: GoogleAuthorizeRequest) =
    path("/o/oauth2/v2/auth")
        .query(clientId.meta.name, req.client.user)
        .query(responseType.meta.name, "code")
        .query(scope.meta.name, req.scopes.joinToString(" "))
        .query(redirectUri.meta.name, req.callbackUri.toString())
        .query(state.meta.name, req.originalUri.query("aft", req.antiForgeryStateToken.state).toString())
        .query(nonce.meta.name, req.nonce)

class GoogleAuth(private val googleClientCredentials: Credentials,
                 private val callbackUri: Uri,
                 private val authScopes: List<String> = listOf("openid", "email"),
                 private val googleBaseUri: Uri = Uri.of("https://accounts.google.com"),
                 private val generateToken: () -> AntiForgeryStateToken = AntiForgeryStateToken.Companion::secure,
                 private val generateNonce: () -> String = { UUID.randomUUID().toString() }
) : Filter {

    override fun invoke(next: HttpHandler): HttpHandler =
        {
            it.cookie("state")?.let {
                // do something here..
                Response(TEMPORARY_REDIRECT).cookie(Cookie("accessToken", ""))
            } ?: redirectToGoogleAuth(it.uri)
        }

    private fun redirectToGoogleAuth(originalUri: Uri): Response {
        val csrf = generateToken()
        val authReq = GoogleAuthorizeRequest(
            googleClientCredentials,
            csrf,
            callbackUri,
            originalUri,
            generateNonce(),
            authScopes
        )
        return Response(TEMPORARY_REDIRECT).with(LOCATION of googleBaseUri.with(authReq))
            .cookie(Cookie("state", csrf.state))
    }
}

