package org.http4k.powerassert

import org.http4k.core.cookie.Cookie
import org.http4k.core.cookie.SameSite.None
import org.http4k.core.cookie.SameSite.Strict
import org.junit.jupiter.api.Test
import java.time.Duration
import java.time.Instant

class CookieMatchersTest {
    @Test
    fun name() {
        assert(Cookie("bob", "value").hasName("bob"))
        assert(!Cookie("bob", "value").hasName("bill"))
    }

    @Test
    fun value() {
        assert(Cookie("name", "bob").hasValue("bob"))
        assert(!Cookie("name", "bob").hasValue("bill"))
    }

    @Test
    fun `value with matcher`() {
        assert(Cookie("name", "bob").hasValue("bob"))
        assert(!Cookie("name", "bill").hasValue("bob"))
    }

    @Test
    fun domain() {
        assert(Cookie("name", "value", domain = "bob").hasDomain("bob"))
        assert(!Cookie("name", "value", domain = "bob").hasDomain("bill"))
    }

    @Test
    fun path() {
        assert(Cookie("name", "value", path = "bob").hasPath("bob"))
        assert(!Cookie("name", "value", path = "bob").hasPath("bill"))
    }

    @Test
    fun secure() {
        assert(Cookie("name", "value", secure = true).isSecure(true))
        assert(!Cookie("name", "value", secure = true).isSecure(false))
    }

    @Test
    fun `http only`() {
        assert(Cookie("name", "value", httpOnly = true).isHttpOnly(true))
        assert(!Cookie("name", "value", httpOnly = true).isHttpOnly(false))
    }

    @Test
    fun expiry() {
        val expires = Instant.EPOCH
        assert(Cookie("name", "value", expires = expires).hasExpiry(expires))
        assert(!Cookie("name", "value", expires = expires).hasExpiry(expires.plus(Duration.ofDays(1))))
    }

    @Test
    fun sameSite() {
        assert(Cookie("name", "value", sameSite = Strict).hasSameSite(Strict))
        assert(!Cookie("name", "value", sameSite = Strict).hasSameSite(None))
    }
}