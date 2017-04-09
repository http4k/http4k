package org.reekwest.http.core.cookie

import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.equalTo
import org.junit.Test
import java.time.LocalDateTime

class CookieTest {
    @Test
    fun creates_full_cookie() {
        val cookie = Cookie("my-cookie", "my-value")
            .comment("a-comment")
            .domain("google.com")
            .maxAge(37)
            .secure()
            .httpOnly()
            .expires(LocalDateTime.of(2017, 3, 11, 12, 15, 21))
        assertThat(cookie.toString(),
            equalTo("""my-cookie="my-value"; Comment=a-comment; Domain=google.com; Max-Age=37; Secure=; HttpOnly=; Expires=Sat, 11 Mar 2017 12:15:21 GMT"""))
    }

    @Test
    fun quotes_cookie_value() {
        assertThat(Cookie("my-cookie", "my \"quoted\" value").toString(),
            equalTo("""my-cookie="my \"quoted\" value"; """))
    }
}