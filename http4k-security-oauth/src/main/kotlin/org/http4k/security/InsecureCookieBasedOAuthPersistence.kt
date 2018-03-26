package org.http4k.security

import org.http4k.core.Request
import org.http4k.core.Response
import org.http4k.core.Status.Companion.FORBIDDEN
import org.http4k.core.cookie.Cookie
import org.http4k.core.cookie.cookie
import org.http4k.core.cookie.invalidateCookie
import java.time.Clock
import java.time.LocalDateTime
import java.time.ZoneId

open class InsecureCookieBasedOAuthPersistence(cookieNamePrefix: String, private val clock: Clock = Clock.systemUTC()) : OAuthPersistence {

    private val csrfName = "${cookieNamePrefix}Csrf"

    private val accessTokenName = "${cookieNamePrefix}AccessToken"

    override fun retrieveCsrf(p1: Request) = p1.cookie(csrfName)?.value

    override fun redirectAuth(redirect: Response, csrf: String) = redirect.cookie(expiring(csrfName, csrf))

    override fun hasToken(request: Request) = request.cookie(accessTokenName) != null

    override fun redirectToken(redirect: Response, accessToken: String) = redirect.cookie(expiring(accessTokenName, accessToken)).invalidateCookie(csrfName)

    override fun failedResponse() = Response(FORBIDDEN).invalidateCookie(csrfName).invalidateCookie(accessTokenName)

    private fun expiring(name: String, value: String) = Cookie(name, value, expires = LocalDateTime.ofInstant(clock.instant().plusSeconds(3600), ZoneId.of("GMT")))
}