package org.http4k.strikt

import org.http4k.core.cookie.Cookie
import org.http4k.core.cookie.SameSite
import org.http4k.strikt.domain
import org.http4k.strikt.expires
import org.http4k.strikt.httpOnly
import org.http4k.strikt.name
import org.http4k.strikt.path
import org.http4k.strikt.sameSite
import org.http4k.strikt.secure
import org.http4k.strikt.value
import org.junit.jupiter.api.Test
import strikt.api.expectThat
import strikt.assertions.isEqualTo
import java.time.LocalDateTime

class CookieAssertionsTest {

    @Test
    fun assertions() {
        val cookie = Cookie(
            name = "bob",
            value = "value",
            domain = "bob",
            expires = LocalDateTime.MAX,
            httpOnly = true,
            secure = true,
            path = "/",
            maxAge = 123L,
            sameSite = SameSite.Lax
        )
        expectThat(cookie) {
            name.isEqualTo(cookie.name)
            value.isEqualTo(cookie.value)
            domain.isEqualTo(cookie.domain)
            expires.isEqualTo(cookie.expires)
            httpOnly.isEqualTo(cookie.httpOnly)
            secure.isEqualTo(cookie.secure)
            path.isEqualTo(cookie.path)
            sameSite.isEqualTo(cookie.sameSite)
        }
    }
}
