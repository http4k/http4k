package org.http4k.security

import org.http4k.core.ContentType.Companion.APPLICATION_FORM_URLENCODED
import org.http4k.core.Credentials
import org.http4k.core.Filter
import org.http4k.core.HttpHandler
import org.http4k.core.Method.POST
import org.http4k.core.Request
import org.http4k.core.Response
import org.http4k.core.Status.Companion.OK
import org.http4k.core.Status.Companion.TEMPORARY_REDIRECT
import org.http4k.core.Uri
import org.http4k.core.body.form
import org.http4k.core.query
import org.http4k.core.then
import org.http4k.core.toParameters
import org.http4k.core.toUrlFormEncoded
import org.http4k.core.with
import org.http4k.filter.ClientFilters
import org.http4k.lens.Header.Common.CONTENT_TYPE
import org.http4k.lens.Header.Common.LOCATION
import java.math.BigInteger
import java.security.SecureRandom

typealias CsrfGenerator = () -> String

data class OAuthConfig(
    private val authBase: Uri,
    val authPath: String,
    val tokenPath: String,
    val credentials: Credentials,
    val apiBase: Uri = authBase) {
    val authUri = authBase.path(authPath)
}

internal class OAuthRedirectionFilter(
    private val clientConfig: OAuthConfig,
    private val callbackUri: Uri,
    private val scopes: List<String>,
    private val generateCrsf: CsrfGenerator = SECURE_GENERATE_RANDOM,
    private val oAuthPersistence: OAuthPersistence
) : Filter {

    override fun invoke(next: HttpHandler): HttpHandler = {
        if (oAuthPersistence.hasToken(it)) next(it) else {
            val csrf = generateCrsf()
            val redirect = Response(TEMPORARY_REDIRECT).with(LOCATION of clientConfig.authUri
                .query("client_id", clientConfig.credentials.user)
                .query("response_type", "code")
                .query("scope", scopes.joinToString(" "))
                .query("redirect_uri", callbackUri.toString())
                .query("state", listOf("csrf" to csrf, "uri" to it.uri.toString()).toUrlFormEncoded())
                .with(oAuthPersistence::modifyState))
            oAuthPersistence.redirectAuth(redirect, csrf)
        }
    }
}

internal class OAuthCallback(
    private val api: HttpHandler,
    private val clientConfig: OAuthConfig,
    private val callbackUri: Uri,
    private val oAuthPersistence: OAuthPersistence
) : HttpHandler {

    private fun codeToAccessToken(code: String) =
        api(Request(POST, clientConfig.tokenPath)
            .with(CONTENT_TYPE of APPLICATION_FORM_URLENCODED)
            .form("grant_type", "authorization_code")
            .form("redirect_uri", callbackUri.toString())
            .form("client_id", clientConfig.credentials.user)
            .form("client_secret", clientConfig.credentials.password)
            .form("code", code))
            .let { if (it.status == OK) it.bodyString() else null }

    override fun invoke(request: Request): Response {
        val state = request.query("state")?.toParameters() ?: emptyList()
        val crsfInState = state.find { it.first == "csrf" }?.second
        return request.query("code")?.let { code ->
            if (crsfInState != null && crsfInState == oAuthPersistence.retrieveCsrf(request)) {
                codeToAccessToken(code)?.let {
                    val originalUri = state.find { it.first == "uri" }?.second ?: "/"
                    oAuthPersistence.redirectToken(Response(TEMPORARY_REDIRECT).header("Location", originalUri), it)
                }
            } else null
        } ?: oAuthPersistence.failedResponse()
    }

}

class OAuth(client: HttpHandler,
            clientConfig: OAuthConfig,
            callbackUri: Uri,
            scopes: List<String>,
            oAuthPersistence: OAuthPersistence,
            generateCrsf: CsrfGenerator = SECURE_GENERATE_RANDOM) {

    val api = ClientFilters.SetHostFrom(clientConfig.apiBase).then(client)

    val authFilter: Filter = OAuthRedirectionFilter(clientConfig, callbackUri, scopes, generateCrsf, oAuthPersistence)

    val callback: HttpHandler = OAuthCallback(api, clientConfig, callbackUri, oAuthPersistence)

    companion object
}

internal val SECURE_GENERATE_RANDOM = { BigInteger(130, SecureRandom()).toString(32) }