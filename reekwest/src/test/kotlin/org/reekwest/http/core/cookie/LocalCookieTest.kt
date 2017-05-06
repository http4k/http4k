package org.reekwest.http.core.cookie

import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.equalTo
import org.junit.Test
import java.time.LocalDateTime

class LocalCookieTest {
    val cookie = Cookie("foo", "bar")

    @Test
    fun `cookie without time attributes does not expire`() {
        assertThat(LocalCookie(cookie, LocalDateTime.MAX).isExpired(LocalDateTime.MIN), equalTo(false))
    }

    @Test
    fun `expiration for cookie with maxAge only`() {
        val created = LocalDateTime.of(2017, 3, 11, 12, 15, 2)
        val localCookie = LocalCookie(cookie.maxAge(5), created)

        assertThat(localCookie.isExpired(created.plusSeconds(5)), equalTo(false))
        assertThat(localCookie.isExpired(created.plusSeconds(6)), equalTo(true))
    }

    @Test
    fun `expiration for cookies with expires only`() {
        val created = LocalDateTime.of(2017, 3, 11, 12, 15, 2)
        val expires = created.plusSeconds(5)
        val localCookie = LocalCookie(cookie.expires(expires), created)

        assertThat(localCookie.isExpired(created.plusSeconds(5)), equalTo(false))
        assertThat(localCookie.isExpired(created.plusSeconds(6)), equalTo(true))
    }
}