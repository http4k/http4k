package org.http4k.security

import org.http4k.core.Request
import org.http4k.core.Response
import org.http4k.core.Status.Companion.FORBIDDEN
import org.http4k.core.Uri
import org.http4k.core.cookie.Cookie
import org.http4k.core.cookie.cookie
import org.http4k.core.cookie.invalidateCookie
import org.http4k.security.openid.IdToken
import java.time.Clock
import java.time.Duration
import java.time.Instant
import java.time.LocalDateTime

/**
 * This is an example implementation which stores CSRF and AccessToken values in an INSECURE client-side cookie.
 * Access-tokens for end-services are fully available to the browser so do not use this in production!
 */
class InsecureCookieBasedOAuthPersistence(cookieNamePrefix: String,
                                          private val cookieValidity: Duration = Duration.ofDays(1),
                                          private val clock: Clock = Clock.systemUTC()) : OAuthPersistence {

    private val csrfName = "${cookieNamePrefix}Csrf"

    private val nonceName = "${cookieNamePrefix}Nonce"

    private val originalUriName = "${cookieNamePrefix}OriginalUri"

    private val accessTokenCookieName = "${cookieNamePrefix}AccessToken"

    override fun retrieveCsrf(request: Request) = request.cookie(csrfName)?.value?.let(::CrossSiteRequestForgeryToken)

    override fun retrieveToken(request: Request): AccessToken? = request.cookie(accessTokenCookieName)?.value?.let { AccessToken(it) }

    override fun retrieveNonce(request: Request): Nonce? =
        request.cookie(nonceName)?.value?.let { Nonce(it) }

    override fun retrieveOriginalUri(request: Request): Uri? = request.cookie(originalUriName)?.value?.let { Uri.of(it) }

    override fun assignCsrf(redirect: Response, csrf: CrossSiteRequestForgeryToken) = redirect.cookie(expiring(csrfName, csrf.value))

    override fun assignToken(request: Request, redirect: Response, accessToken: AccessToken, idToken: IdToken?) =
        redirect.cookie(expiring(accessTokenCookieName, accessToken.value))
        .invalidateCookie(csrfName)
        .invalidateCookie(nonceName)
        .invalidateCookie(originalUriName)

    override fun assignNonce(redirect: Response, nonce: Nonce): Response = redirect.cookie(expiring(nonceName, nonce.value))

    override fun assignOriginalUri(redirect: Response, originalUri: Uri): Response = redirect.cookie(expiring(originalUriName, originalUri.toString()))

    override fun authFailureResponse() = Response(FORBIDDEN)
        .invalidateCookie(csrfName)
        .invalidateCookie(accessTokenCookieName)
        .invalidateCookie(nonceName)
        .invalidateCookie(originalUriName)

    private fun expiring(name: String, value: String) = Cookie(name, value, expires = clock.instant().plus(cookieValidity), path = "/")
}
