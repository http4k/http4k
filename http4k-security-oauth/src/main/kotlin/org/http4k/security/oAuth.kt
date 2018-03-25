package org.http4k.security

import org.http4k.core.ContentType.Companion.APPLICATION_FORM_URLENCODED
import org.http4k.core.Credentials
import org.http4k.core.Filter
import org.http4k.core.HttpHandler
import org.http4k.core.Method.POST
import org.http4k.core.Request
import org.http4k.core.Response
import org.http4k.core.Status.Companion.FORBIDDEN
import org.http4k.core.Status.Companion.OK
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
import org.http4k.lens.Header.Common.CONTENT_TYPE
import org.http4k.lens.Header.Common.LOCATION
import java.math.BigInteger
import java.security.SecureRandom
import java.time.Clock
import java.time.LocalDateTime
import java.time.ZoneId

data class OAuthConfig(
    val serviceName: String,
    private val authBase: Uri,
    val authPath: String,
    val tokenPath: String,
    val credentials: Credentials,
    val apiBase: Uri = authBase) {
    val authUri = authBase.path(authPath)
}

internal class OAuthRedirectionFilter(
    private val clientConfig: OAuthConfig,
    private val csrfName: String,
    private val accessTokenName: String,
    private val callbackUri: Uri,
    private val scopes: List<String>,
    private val clock: Clock,
    private val generateCrsf: () -> String = SECURE_GENERATE_RANDOM,
    private val modifyAuthRedirect: (Uri) -> Uri = { it }) : Filter {

    private fun redirectToAuth(originalUri: Uri) = generateCrsf().let { csrf ->
        val expiry = LocalDateTime.ofInstant(clock.instant().plusSeconds(3600), ZoneId.of("GMT"))

        Response(TEMPORARY_REDIRECT).with(LOCATION of clientConfig.authUri
            .query("client_id", clientConfig.credentials.user)
            .query("response_type", "code")
            .query("scope", scopes.joinToString(" "))
            .query("redirect_uri", callbackUri.toString())
            .query("state", listOf(csrfName to csrf, "uri" to originalUri.toString()).toUrlFormEncoded())
            .with(modifyAuthRedirect))
            .cookie(Cookie(csrfName, csrf, expires = expiry))
    }

    override fun invoke(next: HttpHandler): HttpHandler = { it.cookie(accessTokenName)?.let { _ -> next(it) } ?: redirectToAuth(it.uri) }
}

internal class OAuthCallback(
    private val api: HttpHandler,
    private val clientConfig: OAuthConfig,
    private val callbackUri: Uri,
    private val csrfName: String,
    private val accessTokenName: String,
    private val clock: Clock
) : HttpHandler {
    private fun codeToAccessToken(code: String): String? {
        val accessTokenResponse = api(Request(POST, clientConfig.tokenPath)
            .with(CONTENT_TYPE of APPLICATION_FORM_URLENCODED)
            .form("grant_type", "authorization_code")
            .form("redirect_uri", callbackUri.toString())
            .form("client_id", clientConfig.credentials.user)
            .form("client_secret", clientConfig.credentials.password)
            .form("code", code))

        if (accessTokenResponse.status != OK) return null

        return accessTokenResponse.bodyString()
    }

    override fun invoke(p1: Request): Response {
        val state = p1.query("state")?.toParameters() ?: emptyList()
        val crsfInState = state.find { it.first == csrfName }?.second

        return p1.query("code")?.let { code ->
            if (crsfInState != null && crsfInState == p1.cookie(csrfName)?.value) {
                codeToAccessToken(code)?.let {
                    val originalUri = state.find { it.first == "uri" }?.second ?: "/"
                    val expires = LocalDateTime.ofInstant(clock.instant().plusSeconds(3600), ZoneId.of("GMT"))
                    Response(TEMPORARY_REDIRECT)
                        .header("Location", originalUri)
                        .cookie(Cookie(accessTokenName, it, expires = expires))
                        .invalidateCookie(csrfName)
                }
            } else null
        } ?: Response(FORBIDDEN).invalidateCookie(csrfName).invalidateCookie(accessTokenName)
    }
}

class OAuth(client: HttpHandler,
            clientConfig: OAuthConfig,
            callbackUri: Uri,
            scopes: List<String>,
            clock: Clock = Clock.systemUTC(),
            generateCrsf: () -> String = SECURE_GENERATE_RANDOM,
            modifyAuthRedirect: (Uri) -> Uri = { it }) {

    private val csrfName = "${clientConfig.serviceName}Csrf"
    private val accessTokenName = "${clientConfig.serviceName}AccessToken"

    val api = ClientFilters.SetHostFrom(clientConfig.apiBase).then(client)

    val authFilter: Filter = OAuthRedirectionFilter(clientConfig, csrfName, accessTokenName, callbackUri, scopes, clock, generateCrsf, modifyAuthRedirect)

    val callback: HttpHandler = OAuthCallback(api, clientConfig, callbackUri, csrfName, accessTokenName, clock)

    companion object
}

internal val SECURE_GENERATE_RANDOM = { BigInteger(130, SecureRandom()).toString(32) }