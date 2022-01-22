package org.http4k.security

import com.natpryce.hamkrest.absent
import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.equalTo
import org.http4k.core.Method.GET
import org.http4k.core.Request
import org.http4k.core.Response
import org.http4k.core.Status.Companion.FORBIDDEN
import org.http4k.core.Status.Companion.TEMPORARY_REDIRECT
import org.http4k.core.Uri
import org.http4k.core.cookie.Cookie
import org.http4k.core.cookie.cookie
import org.http4k.core.cookie.invalidateCookie
import org.http4k.util.FixedClock
import org.junit.jupiter.api.Test
import java.time.Clock
import java.time.Duration
import java.time.LocalDateTime

class InsecureCookieBasedOAuthPersistenceTest {

    private val cookieValidity = Duration.ofHours(1)

    private val clock: Clock = FixedClock
    private val persistence = InsecureCookieBasedOAuthPersistence("prefix", cookieValidity, clock)
    private val expectedCookieExpiry = clock.instant().plus(cookieValidity)

    @Test
    fun `failed response has correct cookies`() {
        assertThat(persistence.authFailureResponse(), equalTo(
            Response(FORBIDDEN).invalidateCookie("prefixCsrf").invalidateCookie("prefixAccessToken").invalidateCookie("prefixNonce").invalidateCookie("prefixOriginalUri")
        ))
    }

    @Test
    fun `token retrieval based on cookie`() {
        assertThat(persistence.retrieveToken(Request(GET, "")), absent())
        assertThat(persistence.retrieveToken(Request(GET, "").cookie(Cookie("prefixAccessToken", "tokenValue"))), equalTo(AccessToken("tokenValue")))
    }

    @Test
    fun `csrf retrieval based on cookie`() {
        assertThat(persistence.retrieveCsrf(Request(GET, "")), absent())
        assertThat(persistence.retrieveCsrf(Request(GET, "").cookie(Cookie("prefixCsrf", "csrfValue"))), equalTo(CrossSiteRequestForgeryToken("csrfValue")))
    }

    @Test
    fun `nonce retrieval based on cookie`() {
        assertThat(persistence.retrieveNonce(Request(GET, "")), absent())
        assertThat(persistence.retrieveNonce(Request(GET, "").cookie(Cookie("prefixNonce", "nonceValue"))),
            equalTo(Nonce("nonceValue")))
    }

    @Test
    fun `original uri retrieval based on cookie`() {
        assertThat(persistence.retrieveOriginalUri(Request(GET, "")), absent())
        assertThat(persistence.retrieveOriginalUri(Request(GET, "").cookie(Cookie("prefixOriginalUri", "https://foo.com"))), equalTo(Uri.of("https://foo.com")))
    }

    @Test
    fun `adds csrf as a cookie to the auth redirect`() {
        assertThat(persistence.assignCsrf(Response(TEMPORARY_REDIRECT), CrossSiteRequestForgeryToken("csrfValue")),
            equalTo(Response(TEMPORARY_REDIRECT).cookie(Cookie("prefixCsrf", "csrfValue", expires = expectedCookieExpiry, path = "/"))))
    }

    @Test
    fun `adds csrf as a cookie to the token redirect`() {
        assertThat(persistence.assignToken(Request(GET, ""), Response(TEMPORARY_REDIRECT), AccessToken("tokenValue")),
            equalTo(Response(TEMPORARY_REDIRECT).cookie(Cookie("prefixAccessToken", "tokenValue", expires = expectedCookieExpiry, path = "/"))
                .invalidateCookie("prefixCsrf").invalidateCookie("prefixNonce").invalidateCookie("prefixOriginalUri")
            ))
    }

    @Test
    fun `adds nonce as a cookie to the auth redirect`() {
        assertThat(persistence.assignNonce(Response(TEMPORARY_REDIRECT), Nonce("nonceValue")), equalTo(
            Response(TEMPORARY_REDIRECT).cookie(Cookie("prefixNonce", "nonceValue",
                expires = expectedCookieExpiry, path = "/"))
        ))
    }
}
