package org.http4k.kotest

import io.kotest.matchers.be
import io.kotest.matchers.string.contain
import org.http4k.core.cookie.Cookie
import org.http4k.core.cookie.SameSite.None
import org.http4k.core.cookie.SameSite.Strict
import org.junit.jupiter.api.Test
import java.time.LocalDateTime
import java.time.ZoneOffset

class CookieMatchersTest {
    @Test
    fun name() = assertMatchAndNonMatch(Cookie("bob", "value"), haveName("bob"), haveValue("bill"))

    @Test
    fun value() = assertMatchAndNonMatch(Cookie("name", "bob"), haveValue("bob"), haveValue("bill"))

    @Test
    fun `value with matcher`() {
        assertMatchAndNonMatch(Cookie("name", "bob"), haveValue(be<String>("bob")), haveValue(contain("bill")))
    }

    @Test
    fun domain() = assertMatchAndNonMatch(Cookie("name", "value", domain = "bob"), haveDomain("bob"), haveDomain("bill"))

    @Test
    fun path() = assertMatchAndNonMatch(Cookie("name", "value", path = "bob"), haveCookiePath("bob"), haveCookiePath("bill"))

    @Test
    fun secure() = assertMatchAndNonMatch(Cookie("name", "value", secure = true), beSecure(), beSecure().invert())

    @Test
    fun `http only`() = assertMatchAndNonMatch(Cookie("name", "value", httpOnly = true), beHttpOnly(), beHttpOnly().invert())

    @Test
    fun expireOn() {
        val expires = LocalDateTime.ofEpochSecond(0, 0, ZoneOffset.ofHours(0))
        assertMatchAndNonMatch(Cookie("name", "value", expires = expires),
            expireOn(expires), expireOn(expires.plusDays(1)))
    }

    @Test
    fun `never expire`() {
        assertMatchAndNonMatch(Cookie("name", "value", expires = null),
            neverExpire(), neverExpire().invert())
    }

    @Test
    fun sameSite() = assertMatchAndNonMatch(Cookie("name", "value", sameSite = Strict), haveSameSite(Strict), haveSameSite(None))
}
