package org.reekwest.http.core.cookie

import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.equalTo
import org.junit.Ignore
import org.junit.Test
import org.reekwest.http.core.Parameters
import org.reekwest.http.core.Request.Companion.get
import org.reekwest.http.core.Response
import org.reekwest.http.core.Response.Companion.ok
import java.time.LocalDateTime
import java.time.ZoneOffset

class CookieTest {
    @Test
    fun creates_full_cookie() {
        val cookie = Cookie("my-cookie", "my-value")
            .maxAge(37)
            .expires(LocalDateTime.of(2017, 3, 11, 12, 15, 21))
            .domain("google.com")
            .path("/")
            .secure()
            .httpOnly()
        assertThat(cookie.toString(),
            equalTo("""my-cookie="my-value"; Max-Age=37; Expires=Sat, 11 Mar 2017 12:15:21 GMT; Domain=google.com; Path=/; secure; HttpOnly"""))
    }

    @Test
    fun parses_full_cookie() {
        val original = Cookie("my-cookie", "my-value")
            .maxAge(37)
            .expires(LocalDateTime.of(2017, 3, 11, 12, 15, 21))
            .domain("google.com")
            .path("/")
            .secure()
            .httpOnly()

        val parsed = Cookie.parse(original.toString())

        assertThat(parsed, equalTo(original))
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

    @Test
    fun extracts_cookies_from_response() {
        val cookies = listOf(Cookie("foo", "one"), Cookie("bar", "two").maxAge(3))

        val response = cookies.fold(ok(), Response::cookie)

        assertThat(response.cookies(), equalTo(cookies))
    }

    @Test
    fun parses_cookie_without_quotes(){
        assertThat(Cookie.parse("foo=bar; Path=/"), equalTo(Cookie("foo", "bar").path("/")))
    }

    @Test
    fun invalidates_cookie() {
        assertThat(Cookie("foo", "bar").invalidate(),
            equalTo(Cookie("foo", "").maxAge(0).expires(LocalDateTime.ofEpochSecond(0, 0, ZoneOffset.UTC))))
    }

    @Test
    fun cookie_without_time_info_is_never_expired() {
        assertThat(Cookie("foo", "bar").expired(LocalDateTime.MAX, LocalDateTime.MIN), equalTo(false))
    }

    @Test
    @Ignore("requires cookie to be better represented")
    fun cookie_with_max_age_expiration() {
        val created = LocalDateTime.of(2017, 3, 11, 12, 15, 2)
        assertThat(Cookie("foo", "bar").maxAge(5).expired(created, created.plusSeconds(4)), equalTo(false))
        assertThat(Cookie("foo", "bar").maxAge(5).expired(created, created.plusSeconds(5)), equalTo(true))
    }
}

private fun Cookie.expired(created: LocalDateTime, now: LocalDateTime): Boolean = false