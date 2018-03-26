package org.http4k.security

import com.natpryce.hamkrest.absent
import com.natpryce.hamkrest.equalTo
import com.natpryce.hamkrest.should.shouldMatch
import org.http4k.core.Method.GET
import org.http4k.core.Request
import org.http4k.core.Response
import org.http4k.core.Status.Companion.FORBIDDEN
import org.http4k.core.Status.Companion.TEMPORARY_REDIRECT
import org.http4k.core.cookie.Cookie
import org.http4k.core.cookie.cookie
import org.http4k.core.cookie.invalidateCookie
import org.junit.Test
import java.time.Clock
import java.time.Duration
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId

class InsecureCookieBasedOAuthPersistenceTest {

    private val cookieValidity = Duration.ofHours(2)

    private val clock: Clock = Clock.fixed(Instant.EPOCH, ZoneId.of("UTC"))
    private val persistence = InsecureCookieBasedOAuthPersistence("prefix", cookieValidity, clock)

    @Test
    fun `failed response has correct cookies`() {
        persistence.authFailureResponse() shouldMatch equalTo(
            Response(FORBIDDEN).invalidateCookie("prefixCsrf").invalidateCookie("prefixAccessToken")
        )
    }

    @Test
    fun `token retrieval based on cookie`() {
        persistence.retrieveToken(Request(GET, "")) shouldMatch absent()
        persistence.retrieveToken(Request(GET, "").cookie(Cookie("prefixAccessToken", "tokenValue"))) shouldMatch equalTo("tokenValue")
    }

    @Test
    fun `csrf retrieval based on cookie`() {
        persistence.retrieveCsrf(Request(GET, "")) shouldMatch absent()
        persistence.retrieveCsrf(Request(GET, "").cookie(Cookie("prefixCsrf", "csrfValue"))) shouldMatch equalTo("csrfValue")
    }

    @Test
    fun `adds csrf as a cookie to the auth redirect`() {
        persistence.assignCsrf(Response(TEMPORARY_REDIRECT), "csrfValue") shouldMatch equalTo(
            Response(TEMPORARY_REDIRECT).cookie(Cookie("prefixCsrf", "csrfValue",
                expires = LocalDateTime.now(clock).plus(cookieValidity))))
    }

    @Test
    fun `adds csrf as a cookie to the token redirect`() {
        persistence.assignToken(Response(TEMPORARY_REDIRECT), "tokenValue") shouldMatch equalTo(
            Response(TEMPORARY_REDIRECT).cookie(Cookie("prefixAccessToken", "tokenValue",
                expires = LocalDateTime.now(clock).plus(cookieValidity))).invalidateCookie("prefixCsrf")
        )
    }
}