import GoogleApi.clientId
import GoogleApi.code
import GoogleApi.csrf
import GoogleApi.nonce
import GoogleApi.redirectUri
import GoogleApi.responseType
import GoogleApi.scope
import GoogleApi.state
import org.http4k.core.Body
import org.http4k.core.Credentials
import org.http4k.core.Filter
import org.http4k.core.HttpHandler
import org.http4k.core.Method.POST
import org.http4k.core.Request
import org.http4k.core.Response
import org.http4k.core.Status.Companion.FORBIDDEN
import org.http4k.core.Status.Companion.TEMPORARY_REDIRECT
import org.http4k.core.Uri
import org.http4k.core.cookie.Cookie
import org.http4k.core.cookie.cookie
import org.http4k.core.cookie.invalidateCookie
import org.http4k.core.query
import org.http4k.core.then
import org.http4k.core.with
import org.http4k.filter.ClientFilters
import org.http4k.lens.FormField
import org.http4k.lens.Header.Common.LOCATION
import org.http4k.lens.Query
import org.http4k.lens.Validator.Strict
import org.http4k.lens.WebForm
import org.http4k.lens.uri
import org.http4k.lens.webForm
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

data class OAuthClientConfig(val authBase: Uri,
                             val apiBase: Uri,
                             val client: Credentials,
                             val callbackUri: Uri,
                             val scopes: List<String>) {
    companion object
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
    val code = Query.optional("code")
    val csrf = Query.optional("csrf")
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
data class CodeToAccessTokenRequest(
    val redirectUri: Uri,
    val client: Credentials,
    val code: String) {

    companion object {
        private val grantTypeField = FormField.required("grant_type")
        private val redirectUriField = FormField.uri().required("redirect_uri")
        private val clientIdField = FormField.required("clientId")
        private val clientSecretField = FormField.required("client_secret")
        private val codeField = FormField.required("code")

        private fun from(webForm: WebForm) =
            CodeToAccessTokenRequest(
                redirectUriField(webForm),
                Credentials(clientIdField(webForm), clientSecretField(webForm)),
                codeField(webForm))

        private fun toWebForm(request: CodeToAccessTokenRequest) = WebForm().with(
            grantTypeField.of("authorization_code"),
            redirectUriField.of(request.redirectUri),
            clientIdField.of(request.client.user),
            clientSecretField.of(request.client.password),
            codeField.of(request.code)
        )

        val lens = Body.webForm(Strict).map(CodeToAccessTokenRequest.Companion::from, ::toWebForm).toLens()
    }
}

fun Uri.with(req: GoogleAuthorizeRequest) =
    path("/o/oauth2/v2/auth")
        .query(clientId.meta.name, req.client.user)
        .query(responseType.meta.name, "code")
        .query(scope.meta.name, req.scopes.joinToString(" "))
        .query(redirectUri.meta.name, req.callbackUri.toString())
        .query(state.meta.name, req.originalUri.query("csrf", req.antiForgeryStateToken.state).toString())
        .query(nonce.meta.name, req.nonce)

class OAuth(client: HttpHandler,
            private val oAuthClientConfig: OAuthClientConfig,
            private val generateToken: () -> AntiForgeryStateToken = AntiForgeryStateToken.Companion::secure,
            private val generateNonce: () -> String = { UUID.randomUUID().toString() }) {

    val apiClient = ClientFilters.SetHostFrom(oAuthClientConfig.apiBase).then(client)

    private fun codeToAccessToken(code: String, originalUri: Uri): Response {
        val accessToken = apiClient(Request(POST, "/oauth2/v4/token").with(CodeToAccessTokenRequest.lens of
            CodeToAccessTokenRequest(originalUri, oAuthClientConfig.client, code)))

        println(accessToken.bodyString())
        return Response(TEMPORARY_REDIRECT).cookie(Cookie("accessToken", accessToken.bodyString()))
    }

    private fun redirectToGoogleAuth(originalUri: Uri): Response {
        val csrf = generateToken()
        val authReq = GoogleAuthorizeRequest(
            oAuthClientConfig.client,
            csrf,
            oAuthClientConfig.callbackUri,
            originalUri,
            generateNonce(),
            oAuthClientConfig.scopes
        )
        return Response(TEMPORARY_REDIRECT).with(LOCATION of oAuthClientConfig.authBase.with(authReq))
            .cookie(Cookie("state", csrf.state))
    }

    val authFilter = object : Filter {
        override fun invoke(next: HttpHandler): HttpHandler = { req ->
            req.cookie("accessToken")?.let { next(req) } ?: redirectToGoogleAuth(req.uri)
        }
    }

    val callback: HttpHandler = {
        //http://localhost:9000/callback?state=/?csrf%3Dehnst6i3n89khhn0v27ks6t8gn&code=4/AABog4Jo0BMKTmTKZR87LnGES1U4Q2deF6MbBMvqs4fiDKhTZr0LT0GPYAZ-mBX2gO8JwJZVlvjQ9k_D50K1MIg&authuser=0&session_state=7c0f1ea0f7dff7252ed75788864337e165d75232..05be&prompt=consent#
        code(it)
            ?.let { code -> it.cookie("state")?.let { code to it } }
            ?.let { (code, csrfCookie) ->
                println("hit callback$ $code, $csrfCookie")
                when {
                    csrfCookie.value != csrf(it) -> Response(FORBIDDEN).invalidateCookie("state")
                    else -> codeToAccessToken(code, Uri.of(""))
                }
            } ?: Response(FORBIDDEN)
    }

}

fun OAuthClientConfig.Companion.google(
    credentials: Credentials = Credentials(System.getenv("CLIENT_ID"), System.getenv("CLIENT_SECRET")),
    callbackUri: Uri = Uri.of("http://localhost:9000/callback")
) = OAuthClientConfig(Uri.of("https://accounts.google.com"),
    Uri.of("https://www.googleapis.com"),
    credentials,
    callbackUri,
    listOf("openid", "email"))
