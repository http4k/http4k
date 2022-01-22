package org.http4k.hamkrest

import com.natpryce.hamkrest.containsSubstring
import com.natpryce.hamkrest.equalTo
import org.http4k.core.cookie.Cookie
import org.http4k.core.cookie.SameSite.None
import org.http4k.core.cookie.SameSite.Strict
import org.junit.jupiter.api.Test
import java.time.Duration
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneOffset

class CookieMatchersTest {
    @Test
    fun name() = assertMatchAndNonMatch(Cookie("bob", "value"), hasCookieName("bob"), hasCookieValue("bill"))

    @Test
    fun value() = assertMatchAndNonMatch(Cookie("name", "bob"), hasCookieValue("bob"), hasCookieValue("bill"))

    @Test
    fun `value with matcher`() {
        assertMatchAndNonMatch(Cookie("name", "bob"), hasCookieValue(equalTo("bob")), hasCookieValue(containsSubstring("bill")))
    }

    @Test
    fun domain() = assertMatchAndNonMatch(Cookie("name", "value", domain = "bob"), hasCookieDomain("bob"), hasCookieDomain("bill"))

    @Test
    fun path() = assertMatchAndNonMatch(Cookie("name", "value", path = "bob"), hasCookiePath("bob"), hasCookiePath("bill"))

    @Test
    fun secure() = assertMatchAndNonMatch(Cookie("name", "value", secure = true), isSecureCookie(true), isSecureCookie(false))

    @Test
    fun `http only`() = assertMatchAndNonMatch(Cookie("name", "value", httpOnly = true), isHttpOnlyCookie(true), isHttpOnlyCookie(false))

    @Test
    fun expiry() {
        val expires = Instant.EPOCH
        assertMatchAndNonMatch(Cookie("name", "value", expires = expires),
            hasCookieExpiry(expires), hasCookieExpiry(expires.plus(Duration.ofDays(1))))
    }

    @Test
    fun sameSite() = assertMatchAndNonMatch(Cookie("name", "value", sameSite = Strict), hasCookieSameSite(Strict), hasCookieSameSite(None))
}
