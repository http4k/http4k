package org.http4k.hamkrest

import org.http4k.core.cookie.Cookie
import org.junit.jupiter.api.Test
import java.time.LocalDateTime
import java.time.ZoneOffset

class CookieMatchersTest {
    @Test
    fun `name`() = assertMatchAndNonMatch(Cookie("bob", "value"), hasCookieName("bob"), hasCookieValue("bill"))

    @Test
    fun `value`() = assertMatchAndNonMatch(Cookie("name", "bob"), hasCookieValue("bob"), hasCookieValue("bill"))

    @Test
    fun `domain`() = assertMatchAndNonMatch(Cookie("name", "value", domain = "bob"), hasCookieDomain("bob"), hasCookieDomain("bill"))

    @Test
    fun `path`() = assertMatchAndNonMatch(Cookie("name", "value", path = "bob"), hasCookiePath("bob"), hasCookiePath("bill"))

    @Test
    fun `secure`() = assertMatchAndNonMatch(Cookie("name", "value", secure = true), isSecureCookie(true), isSecureCookie(false))

    @Test
    fun `http only`() = assertMatchAndNonMatch(Cookie("name", "value", httpOnly = true), isHttpOnlyCookie(true), isHttpOnlyCookie(false))

    @Test
    fun `expiry`() {
        val expires = LocalDateTime.ofEpochSecond(0, 0, ZoneOffset.ofHours(0))
        assertMatchAndNonMatch(Cookie("name", "value", expires = expires),
            hasCookieExpiry(expires), hasCookieExpiry(expires.plusDays(1)))
    }

}