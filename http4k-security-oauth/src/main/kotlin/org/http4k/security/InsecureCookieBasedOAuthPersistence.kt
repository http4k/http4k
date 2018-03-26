package org.http4k.security

import org.http4k.core.Request
import org.http4k.core.Response
import org.http4k.core.Status.Companion.FORBIDDEN
import org.http4k.core.cookie.Cookie
import org.http4k.core.cookie.cookie
import org.http4k.core.cookie.invalidateCookie
import java.time.Clock
import java.time.Duration
import java.time.LocalDateTime
import java.time.ZoneId

/**
 * This is an example implementation which stores CSRF and AccessToken values in an INSECURE client-side cookie.
 */
open class InsecureCookieBasedOAuthPersistence(cookieNamePrefix: String,
                                               private val cookieValidity: Duration = Duration.ofHours(1),
                                               private val clock: Clock = Clock.systemUTC()) : OAuthPersistence {

    private val csrfName = "${cookieNamePrefix}Csrf"

    private val accessTokenCookieName = "${cookieNamePrefix}AccessToken"

    override fun retrieveCsrf(p1: Request) = p1.cookie(csrfName)?.value

    override fun retrieveToken(p1: Request): String? = p1.cookie(accessTokenCookieName)?.value

    override fun assignCsrf(redirect: Response, csrf: String) = redirect.cookie(expiring(csrfName, csrf))

    override fun assignToken(redirect: Response, accessToken: String) = redirect.cookie(expiring(accessTokenCookieName, accessToken)).invalidateCookie(csrfName)

    override fun authFailureResponse() = Response(FORBIDDEN).invalidateCookie(csrfName).invalidateCookie(accessTokenCookieName)

    private fun expiring(name: String, value: String) = Cookie(name, value, expires = LocalDateTime.ofInstant(clock.instant().plus(cookieValidity), ZoneId.of("GMT")))
}