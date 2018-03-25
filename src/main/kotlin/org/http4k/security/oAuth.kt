package org.http4k.security

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
import org.http4k.core.toParameters
import org.http4k.core.toUrlFormEncoded
import org.http4k.core.with
import org.http4k.filter.ClientFilters
import org.http4k.lens.Header.Common.LOCATION
import java.math.BigInteger
import java.security.SecureRandom
import java.util.UUID

data class OAuthConfig(
    val serviceName: String,
    private val authBase: Uri,
    val authPath: String,
    val apiBase: Uri,
    val tokenPath: String,
    val credentials: Credentials) {
    val authUri = authBase.path(authPath)
}

internal class OAuthRedirectionFilter(
    private val clientConfig: OAuthConfig,
    private val csrfName: String,
    private val accessTokenName: String,
    private val callbackUri: Uri,
    private val scopes: List<String>,
    private val generateCrsf: () -> String,
    private val generateNonce: () -> String) : Filter {
    private fun redirectToAuth(originalUri: Uri) = generateCrsf().let { csrf ->
        Response(TEMPORARY_REDIRECT).with(LOCATION of clientConfig.authUri
            .query("client_id", clientConfig.credentials.user)
            .query("response_type", "code")
            .query("scope", scopes.joinToString(" "))
            .query("redirect_uri", callbackUri.toString())
            .query("state", listOf(csrfName to csrf, "uri" to originalUri.toString()).toUrlFormEncoded())
            .query("nonce", generateNonce()))
            .cookie(Cookie(csrfName, csrf))
    }

    override fun invoke(next: HttpHandler): HttpHandler = { it.cookie(accessTokenName)?.let { _ -> next(it) } ?: redirectToAuth(it.uri) }
}

internal class OAuthCallback(
    private val api: HttpHandler,
    private val clientConfig: OAuthConfig,
    private val callbackUri: Uri,
    private val csrfName: String,
    private val accessTokenName: String
) : HttpHandler {
    /**
     * POST www.googleapis.com/oauth2/v4/token
    //    Content-Type: application/x-www-form-urlencoded
    //    code=4/P7q7W91a-oMsCeLvIaQm6bTrgtp7&
    //    client_id=8819981768.apps.googleusercontent.com&
    //    client_secret={client_secret}&
    //    redirect_uri=https://oauth2-login-demo.example.com/code&
    //    grant_type=authorization_code
     */

    private fun codeToAccessToken(code: String): Response {
        val accessTokenResponse = api(Request(POST, clientConfig.tokenPath)
            .form("grant_type", "authorization_code")
            .form("redirect_uri", callbackUri.toString())
            .form("client_id", clientConfig.credentials.user)
            .form("client_secret", clientConfig.credentials.password)
            .form("code", code))

        // this is wrong!
        println(accessTokenResponse.bodyString())

        return Response(TEMPORARY_REDIRECT).cookie(Cookie(accessTokenName, accessTokenResponse.bodyString()))
    }
    override fun invoke(p1: Request) = p1.query("code")
        ?.let { code ->
            p1.query("state")
                ?.let { it to code }
                ?.let { (state, code) -> p1.cookie(csrfName)?.let { Triple(it.value, state.toParameters(), code) } }
        }
        ?.let { (csrfCookie, state, code) ->
            if (csrfCookie == state.find { it.first == csrfName }?.second) codeToAccessToken(code)
            else null
        }
        ?: Response(FORBIDDEN).invalidateCookie(csrfName)
}

class OAuth(client: HttpHandler,
            clientConfig: OAuthConfig,
            callbackUri: Uri,
            scopes: List<String>,
            generateCrsf: () -> String = { BigInteger(130, SecureRandom()).toString(32) },
            generateNonce: () -> String = { UUID.randomUUID().toString() }) {

    private val csrfName = "${clientConfig.serviceName}Csrf"
    private val accessTokenName = "${clientConfig.serviceName}AccessToken"

    val api = ClientFilters.SetHostFrom(clientConfig.apiBase).then(client)

    val authFilter: Filter = OAuthRedirectionFilter(clientConfig, csrfName, accessTokenName, callbackUri, scopes, generateCrsf, generateNonce)

    val callback: HttpHandler = OAuthCallback(api, clientConfig, callbackUri, csrfName, accessTokenName)

    companion object
}