package org.reekwest.http.core.cookie

import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.equalTo
import org.junit.Test
import org.reekwest.http.core.Parameters
import org.reekwest.http.core.get
import org.reekwest.http.core.header
import org.reekwest.http.core.ok
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

    @Test
    fun add_cookie_to_response() {
        val cookie = Cookie("my-cookie", "my value")
        val response = ok().cookie(cookie)
        assertThat(response.headers, equalTo(listOf("Set-Cookie" to cookie.toString()) as Parameters))
    }

    @Test
    fun remove_cookie_from_response() {
        val response = ok()
            .header("Set-Cookie", "other-cookie=\"other-value\"")
            .header("Set-Cookie", "a-cookie=\"a-value\"")
            .header("Other-Header", "other-value")
            .removeCookie("a-cookie")
        assertThat(response.headers, equalTo(listOf(
            "Set-Cookie" to "other-cookie=\"other-value\"",
            "Other-Header" to "other-value"
        ) as Parameters))
    }

    @Test
    fun replace_cookie_in_response() {
        val cookie = Cookie("my-cookie", "my value")
        val replacement = Cookie("my-cookie", "my second value")
        val response = ok().cookie(cookie).replaceCookie(replacement)
        assertThat(response.headers, equalTo(listOf("Set-Cookie" to replacement.toString()) as Parameters))
    }

    @Test
    fun store_and_extract_cookie_from_request() {
        val request = get("ignore").cookie("foo", "bar")
        assertThat(request.headers, equalTo(listOf("Cookie" to "foo=\"bar\"; ") as Parameters))
    }

    @Test
    fun multiple_request_cookies_are_stored_in_same_header() {
        val request = get("ignore").cookie("foo", "one").cookie("bar", "two")
        assertThat(request.headers, equalTo(listOf("Cookie" to "foo=\"one\"; bar=\"two\"; ") as Parameters))
    }
}