package org.http4k.filter.cookie

import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.equalTo
import org.http4k.core.cookie.Cookie
import org.junit.jupiter.api.Test
import java.time.Duration
import java.time.LocalDateTime
import java.time.LocalDateTime.MAX
import java.time.LocalDateTime.MIN
import java.time.ZoneOffset.UTC

class LocalCookieTest {
    val cookie = Cookie("foo", "bar")

    @Test
    fun `cookie without time attributes does not expire`() {
        assertThat(LocalCookie(cookie, MAX.toInstant(UTC)).isExpired(MIN.toInstant(UTC)), equalTo(false))
    }

    @Test
    fun `cookie with maxAge zero expires straight away`() {
        val created = LocalDateTime.of(2017, 3, 11, 12, 15, 2)
        val localCookie = LocalCookie(cookie.maxAge(0), created.toInstant(UTC))

        assertThat(localCookie.isExpired(created.plus(Duration.ofMillis(500)).toInstant(UTC)), equalTo(true))
    }

    @Test
    fun `expiration for cookie with maxAge only`() {
        val created = LocalDateTime.of(2017, 3, 11, 12, 15, 2)
        val localCookie = LocalCookie(cookie.maxAge(5), created.toInstant(UTC))

        assertThat(localCookie.isExpired(created.plusSeconds(4).toInstant(UTC)), equalTo(false))
        assertThat(localCookie.isExpired(created.plusSeconds(5).toInstant(UTC)), equalTo(true))
    }

    @Test
    fun `expiration for cookies with expires only`() {
        val created = LocalDateTime.of(2017, 3, 11, 12, 15, 2).toInstant(UTC)
        val expires = created.plusSeconds(5)
        val localCookie = LocalCookie(cookie.expires(expires), created)

        assertThat(localCookie.isExpired(created.plusSeconds(5)), equalTo(false))
        assertThat(localCookie.isExpired(created.plusSeconds(6)), equalTo(true))
    }
}
