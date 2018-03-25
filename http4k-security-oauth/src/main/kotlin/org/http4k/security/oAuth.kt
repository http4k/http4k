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

typealias ModifyAuthRedirectUri = (Uri) -> Uri
typealias CsrfGenerator = () -> String

data class OAuthConfig(
    val serviceName: String,
    private val authBase: Uri,
    val authPath: String,
    val tokenPath: String,
    val credentials: Credentials,
    val apiBase: Uri = authBase) {
    val authUri = authBase.path(authPath)
    val csrfName = serviceName + "Csrf"
}

internal class OAuthRedirectionFilter(
    private val clientConfig: OAuthConfig,
    private val callbackUri: Uri,
    private val scopes: List<String>,
    private val clock: Clock,
    private val generateCrsf: CsrfGenerator = SECURE_GENERATE_RANDOM,
    private val modifyAuthRedirect: ModifyAuthRedirectUri = { it },
    private val isAuthed: (Request) -> Boolean
) : Filter {

    override fun invoke(next: HttpHandler): HttpHandler = {
        if (isAuthed(it)) next(it) else {
            val csrf = generateCrsf()
            val redirect = Response(TEMPORARY_REDIRECT).with(LOCATION of clientConfig.authUri
                .query("client_id", clientConfig.credentials.user)
                .query("response_type", "code")
                .query("scope", scopes.joinToString(" "))
                .query("redirect_uri", callbackUri.toString())
                .query("state", listOf(clientConfig.csrfName to csrf, "uri" to it.uri.toString()).toUrlFormEncoded())
                .with(modifyAuthRedirect))
            val expiry = LocalDateTime.ofInstant(clock.instant().plusSeconds(3600), ZoneId.of("GMT"))
            redirect.cookie(Cookie(clientConfig.csrfName, csrf, expires = expiry))
        }
    }
}

internal class OAuthCallback(
    private val api: HttpHandler,
    private val clientConfig: OAuthConfig,
    private val callbackUri: Uri,
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
        val crsfInState = state.find { it.first == clientConfig.csrfName }?.second

        return p1.query("code")?.let { code ->
            if (crsfInState != null && crsfInState == p1.cookie(clientConfig.csrfName)?.value) {
                codeToAccessToken(code)?.let {
                    val originalUri = state.find { it.first == "uri" }?.second ?: "/"
                    val redirect = Response(TEMPORARY_REDIRECT)
                        .header("Location", originalUri)
                        .invalidateCookie(clientConfig.csrfName)

                    val expires = LocalDateTime.ofInstant(clock.instant().plusSeconds(3600), ZoneId.of("GMT"))
                    redirect.cookie(Cookie(accessTokenName, it, expires = expires))
                }
            } else null
        } ?: Response(FORBIDDEN).invalidateCookie(clientConfig.csrfName).invalidateCookie(accessTokenName)
    }
}

class OAuth(client: HttpHandler,
            clientConfig: OAuthConfig,
            callbackUri: Uri,
            scopes: List<String>,
            clock: Clock = Clock.systemUTC(),
            generateCrsf: CsrfGenerator = SECURE_GENERATE_RANDOM,
            modifyAuthRedirect: ModifyAuthRedirectUri = { it }) {

    private val accessTokenName = "${clientConfig.serviceName}AccessToken"

    val api = ClientFilters.SetHostFrom(clientConfig.apiBase).then(client)

    val authFilter: Filter = OAuthRedirectionFilter(clientConfig, callbackUri, scopes, clock, generateCrsf, modifyAuthRedirect) { req: Request -> req.cookie(accessTokenName) != null }

    val callback: HttpHandler = OAuthCallback(api, clientConfig, callbackUri, accessTokenName, clock)

    companion object
}

internal val SECURE_GENERATE_RANDOM = { BigInteger(130, SecureRandom()).toString(32) }