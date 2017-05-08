package org.http4k.core.cookie

import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.equalTo
import org.http4k.core.Parameters
import org.http4k.core.Request.Companion.get
import org.http4k.core.Response
import org.http4k.core.Response.Companion.ok
import org.junit.Test
import java.time.LocalDateTime
import java.time.ZoneOffset

class CookieTest {

    @Test
    fun `full cookie creation`() {
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
    fun `cookie creation and parsing round trip`() {
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
    fun `cookie values are quoted`() {
        assertThat(Cookie("my-cookie", "my \"quoted\" value").toString(),
            equalTo("""my-cookie="my \"quoted\" value"; """))
    }

    @Test
    fun `cookies can be added to the response`() {
        val cookie = Cookie("my-cookie", "my value")

        val response = ok().cookie(cookie)

        assertThat(response.headers, equalTo(listOf("Set-Cookie" to cookie.toString()) as Parameters))
    }

    @Test
    fun `cookies can be removed from the response`() {
        val response = ok()
            .header("Set-Cookie", "other-cookie=\"other-value\"")
            .header("Set-Cookie", "a-cookie=\"a-value\"")
            .header("Other-Header", "other-value")
            .removeCookie("a-cookie")

        assertThat(response.headers, equalTo(listOf(
            "Other-Header" to "other-value",
            "Set-Cookie" to "other-cookie=\"other-value\""
        ) as Parameters))
    }

    @Test
    fun `cookies can be replaced in the response`() {
        val cookie = Cookie("my-cookie", "my value")
        val replacement = Cookie("my-cookie", "my second value")

        val response = ok().cookie(cookie).replaceCookie(replacement)

        assertThat(response.headers, equalTo(listOf("Set-Cookie" to replacement.toString()) as Parameters))
    }

    @Test
    fun `cookes can be stored in request`() {
        val request = get("ignore").cookie("foo", "bar")

        assertThat(request.headers, equalTo(listOf("Cookie" to "foo=\"bar\"; ") as Parameters))
    }

    @Test
    fun `cookes can be retrieved from request`() {
        val request = get("ignore").header("Cookie", "foo=\"bar\"; ")

        assertThat(request.cookie("foo"), equalTo(Cookie("foo", "bar")))
    }

    @Test
    fun `request stores multiple cookies in single header`() {
        val request = get("ignore").cookie("foo", "one").cookie("bar", "two")

        assertThat(request.headers, equalTo(listOf("Cookie" to "foo=\"one\"; bar=\"two\"; ") as Parameters))
    }

    @Test
    fun `cookies can be extracted from response`() {
        val cookies = listOf(Cookie("foo", "one"), Cookie("bar", "two").maxAge(3))

        val response = cookies.fold(ok(), Response::cookie)

        assertThat(response.cookies(), equalTo(cookies))
    }

    @Test
    fun `cookie without quoted value can be parsed`() {
        assertThat(Cookie.parse("foo=bar; Path=/"), equalTo(Cookie("foo", "bar").path("/")))
    }

    @Test
    fun `cookie can be invalidated`() {
        assertThat(Cookie("foo", "bar").invalidate(),
            equalTo(Cookie("foo", "").maxAge(0).expires(LocalDateTime.ofEpochSecond(0, 0, ZoneOffset.UTC))))
    }
}
