package org.http4k.core.cookie

import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.containsSubstring
import com.natpryce.hamkrest.equalTo
import org.http4k.core.Method
import org.http4k.core.Method.*
import org.http4k.core.Parameters
import org.http4k.core.Request
import org.http4k.core.Response
import org.http4k.core.Status
import org.http4k.core.cookie.SameSite.Lax
import org.junit.jupiter.api.Test
import java.time.LocalDateTime
import java.time.ZoneOffset
import java.util.Locale

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
            .sameSite(Lax)

        assertThat(cookie.toString(),
            equalTo("""my-cookie="my-value"; Max-Age=37; Expires=Sat, 11 Mar 2017 12:15:21 GMT; Domain=google.com; Path=/; secure; HttpOnly; SameSite=Lax"""))
    }

    @Test
    fun `cookie expiry date is always in english`() {
        val currentSystemLocale = Locale.getDefault()
        try {
            Locale.setDefault(Locale.TAIWAN)
            val cookie = Cookie("foo", "bar").expires(LocalDateTime.of(2017, 3, 11, 12, 15, 21))

            assertThat(cookie.toString(), containsSubstring("""Expires=Sat, 11 Mar 2017 12:15:21 GMT"""))
        } finally {
            Locale.setDefault(currentSystemLocale)
        }
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
            .sameSite(Lax)

        val parsed = Cookie.parse(original.toString())

        assertThat(parsed, equalTo(original))
    }

    @Test
    fun `can parse cookie with ending with semicolon`() {
        assertThat(Cookie.parse("foo=bar;"), equalTo(Cookie("foo", "bar")))
    }

    @Test
    fun `cookies can be extracted from request`() {
        assertThat(Request(GET, "/").cookie("foo", "bar").cookies(), equalTo(listOf(Cookie("foo", "bar"))))
    }

    @Test
    fun `cookies with ending semicolon can be extracted from request`() {
        assertThat(Request(GET, "/").header("cookie", "foo=\"bar\";").cookies(), equalTo(listOf(Cookie("foo", "bar"))))
    }

    @Test
    fun `cookie values are quoted`() {
        assertThat(Cookie("my-cookie", "my \"quoted\" value").toString(),
            equalTo("""my-cookie="my \"quoted\" value"; """))
    }

    @Test
    fun `cookies with equals signs inside quotes can be extracted from request`() {
        assertThat(Request(GET, "/").header("cookie", "foo=\"bar==\";").cookies(), equalTo(listOf(Cookie("foo", "bar=="))))
        assertThat(Request(GET, "/").header("cookie", "foo=\"==bar==\";").cookies(), equalTo(listOf(Cookie("foo", "==bar=="))))
        assertThat(Request(GET, "/").header("cookie", "foo=\"==bar\";").cookies(), equalTo(listOf(Cookie("foo", "==bar"))))
    }

    @Test
    fun `cookies can be added to the response`() {
        val cookie = Cookie("my-cookie", "my value")

        val response = Response(Status.OK).cookie(cookie)

        assertThat(response.headers, equalTo(listOf("Set-Cookie" to cookie.toString()) as Parameters))
    }

    @Test
    fun `cookies can be removed from the response`() {
        val response = Response(Status.OK)
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
    fun `cookies can be removed from the request`() {
        val request = Request(GET, "")
            .header("Cookie", "other-cookie=\"other-value\"")
            .header("Cookie", "a-cookie=\"a-value\"")
            .header("Other-Header", "other-value")
            .removeCookie("a-cookie")

        assertThat(request.headers, equalTo(listOf(
            "Other-Header" to "other-value",
            "Cookie" to "other-cookie=\"other-value\""
        ) as Parameters))
    }

    @Test
    fun `cookies can be replaced in the response`() {
        val cookie = Cookie("my-cookie", "my value")
        val replacement = Cookie("my-cookie", "my second value")

        val response = Response(Status.OK).cookie(cookie).replaceCookie(replacement)

        assertThat(response.headers, equalTo(listOf("Set-Cookie" to replacement.toString()) as Parameters))
    }

    @Test
    fun `cookies can be stored in request`() {
        val request = Request(GET, "ignore").cookie("foo", "bar")

        assertThat(request.headers, equalTo(listOf("Cookie" to "foo=\"bar\"") as Parameters))
    }

    @Test
    fun `cookies can be retrieved from request`() {
        val request = Request(GET, "ignore").header("Cookie", "foo=\"bar\"")

        assertThat(request.cookie("foo"), equalTo(Cookie("foo", "bar")))
    }

    @Test
    fun `request stores multiple cookies in single header`() {
        val request = Request(GET, "ignore").cookie("foo", "one").cookie("bar", "two")

        assertThat(request.headers, equalTo(listOf("Cookie" to """foo="one"; bar="two"""") as Parameters))
    }

    @Test
    fun `request can store cookies with special characters`() {
        val request = Request(GET, "ignore").cookie("foo", "\"one\"").cookie("bar", "two=three")

        assertThat(request.headers, equalTo(listOf("Cookie" to """foo="\"one\""; bar="two=three"""") as Parameters))
    }

    @Test
    fun `cookies can be extracted from response`() {
        val cookies = listOf(Cookie("foo", "one"), Cookie("bar", "two").maxAge(3))

        val response = cookies.fold(Response(Status.OK), Response::cookie)

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

    @Test
    fun `cookie can be invalidated at response level`() {
        assertThat(Response(Status.OK).cookie(Cookie("foo", "bar").maxAge(10)).invalidateCookie("foo").cookies().first(),
            equalTo(Cookie("foo", "").invalidate()))
    }

    @Test
    fun `cookie with domain can be invalidated at response level`() {
        assertThat(Response(Status.OK).cookie(Cookie("foo", "bar", domain = "foo.com").maxAge(10)).invalidateCookie("foo", "foo.com").cookies().first(),
            equalTo(Cookie("foo", "", domain = "foo.com").invalidate()))
    }

    @Test
    fun `cookie with various expires date formats parsed`() {
        val expected = Cookie("foo", "bar").expires(LocalDateTime.of(2017, 3, 11, 12, 15, 21))

        assertThat(Cookie.parse("foo=bar; Expires=Sat, 11 Mar 2017 12:15:21 GMT"), equalTo(expected))
        assertThat(Cookie.parse("foo=bar; Expires=Sat, 11-Mar-2017 12:15:21 GMT"), equalTo(expected))
        assertThat(Cookie.parse("foo=bar; Expires=Sat, 11-Mar-17 12:15:21 GMT"), equalTo(expected))
        assertThat(Cookie.parse("foo=bar; Expires=Sat, 11 Mar 17 12:15:21 GMT"), equalTo(expected))
        assertThat(Cookie.parse("foo=bar; Expires=Sat Mar 11 17 12:15:21 GMT"), equalTo(expected))
        assertThat(Cookie.parse("foo=bar; Expires=Sat Mar 11 2017 12:15:21 GMT"), equalTo(expected))
        assertThat(Cookie.parse("foo=bar; Expires=anything else"), equalTo(Cookie("foo", "bar")))
    }

    @Test
    fun `ignores unrecognized SameSite attribute`() {
        val expected = Cookie("foo", "bar")

        assertThat(Cookie.parse("foo=bar; SameSite=Unknown"), equalTo(expected))
    }
}
