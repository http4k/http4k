package org.http4k.filter

import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.equalTo
import org.http4k.core.Method.GET
import org.http4k.core.Request
import org.http4k.core.Response
import org.http4k.core.Status.Companion.OK
import org.http4k.core.Uri
import org.http4k.core.cookie.Cookie
import org.http4k.core.cookie.cookie
import org.http4k.core.then
import org.http4k.filter.cookie.LocalCookie
import org.http4k.filter.cookie.DefaultCookieStorage
import org.http4k.hamkrest.hasBody
import org.http4k.hamkrest.hasHeader
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.fail
import java.time.Clock
import java.time.Duration
import java.time.Instant
import java.time.ZoneId
import java.time.ZoneOffset.UTC

class ClientCookiesTest {

    @Test
    fun `can store and send cookies across multiple calls`() {
        val server = { request: Request -> Response(OK).counterCookie(request.counterCookie() + 1) }

        val client = ClientFilters.Cookies().then(server)

        (0..3).forEach {
            val response = client(Request(GET, "http://example.com/"))
            assertThat(response, hasHeader("Set-Cookie", """counter="${it + 1}""""))
        }
    }

    @Test
    fun `expired cookies are removed from storage and not sent`() {
        val server = { request: Request ->
            when (request.uri.path) {
                "/set" -> Response(OK).cookie(Cookie("foo", "bar", 5))
                else -> Response(OK).body(request.cookie("foo")?.value ?: "gone")
            }
        }

        val cookieStorage = DefaultCookieStorage()

        val clock = object : Clock() {
            var millis: Long = 0
            override fun withZone(zone: ZoneId?): Clock = TODO()
            override fun getZone(): ZoneId = ZoneId.of("GMT")
            override fun instant(): Instant = Instant.ofEpochMilli(millis)
            fun add(seconds: Int) {
                millis += seconds * 1000
            }
        }

        val client = ClientFilters.Cookies(clock, cookieStorage).then(server)

        client(Request(GET, "/set"))

        assertThat(cookieStorage.retrieve(Uri.of("")).size, equalTo(1))

        assertThat(client(Request(GET, "/get")), hasBody("bar"))

        clock.add(10)

        assertThat(client(Request(GET, "/get")), hasBody("gone"))
    }

    @Test
    fun `cookie expiry uses the same timezone as cookie parsing`() {
        val zoneId = ZoneId.of("Europe/London")

        val cookie = Cookie.parse("foo=bar;Path=/;Expires=Thu, 25-Oct-2018 10:00:00 GMT;HttpOnly")
            ?: fail("Couldn't parse cookie")
        // this was 11:00:00 in Europe/London due to daylight savings

        val server = { request: Request ->
            when (request.uri.path) {
                "/set" -> Response(OK).cookie(cookie)
                else -> Response(OK).body(request.cookie("foo")?.value ?: "gone")
            }
        }

        val cookieStorage = DefaultCookieStorage()

        val clock = object : Clock() {
            var instant = cookie.expires!!.atZone(UTC).toInstant() - Duration.ofMinutes(50)
            override fun withZone(zone: ZoneId?): Clock = TODO()
            override fun getZone(): ZoneId = zoneId
            override fun instant(): Instant = instant
            fun add(hours: Long) {
                instant += Duration.ofHours(hours)
            }
        }

        val client = ClientFilters.Cookies(clock, cookieStorage).then(server)

        client(Request(GET, "/set"))

        assertThat(cookieStorage.retrieve(Uri.of("")).size, equalTo(1))

        // if the parser uses UTC and the expiry checker uses local time then this will be 'gone'
        assertThat(client(Request(GET, "/get")), hasBody("bar"))

        clock.add(1)

        // if the parser uses local time and the expiry checker uses UTC then this will be 'bar'
        assertThat(client(Request(GET, "/get")), hasBody("gone"))
    }

    // ── RFC 6265 cross-origin isolation (ClientFilters.Cookies integration) ──

    @Test
    fun `RFC6265 - cookies set by site A are not sent to site B`() {
        val server = { request: Request ->
            when (request.uri.host) {
                "site-a.com" -> Response(OK).cookie(Cookie("secret", "fromA"))
                else -> Response(OK).body(request.cookie("secret")?.value ?: "none")
            }
        }

        val client = ClientFilters.Cookies(storage = DefaultCookieStorage()).then(server)

        // First call to site-a.com causes Set-Cookie from that origin
        client(Request(GET, "https://site-a.com/"))
        // Second call to site-b.com must NOT receive the cookie set by site-a.com
        val response = client(Request(GET, "https://site-b.com/"))
        assertThat(response, hasBody("none"))
    }

    @Test
    fun `RFC6265 - secure cookie is not forwarded over plain http`() {
        val storage = DefaultCookieStorage()
        storage.store(
            listOf(
                LocalCookie(
                    Cookie("tok", "secret", secure = true),
                    Instant.EPOCH,
                    Uri.of("https://example.com/")
                )
            )
        )

        val overHttps = storage.retrieve(Uri.of("https://example.com/"))
        val overHttp = storage.retrieve(Uri.of("http://example.com/"))

        assertThat(overHttps.size, equalTo(1))
        assertThat(overHttp.size, equalTo(0))
    }

    @Test
    fun `RFC6265 - domain cookie is sent to subdomain`() {
        val server = { request: Request ->
            when (request.uri.host) {
                "example.com" -> Response(OK).cookie(Cookie("global", "yes", domain = "example.com"))
                else -> Response(OK).body(request.cookie("global")?.value ?: "none")
            }
        }

        val client = ClientFilters.Cookies(storage = DefaultCookieStorage()).then(server)

        client(Request(GET, "https://example.com/"))
        val response = client(Request(GET, "https://api.example.com/"))
        assertThat(response, hasBody("yes"))
    }

    fun Request.counterCookie() = cookie("counter")?.value?.toInt() ?: 0
    fun Response.counterCookie(value: Int) = cookie(Cookie("counter", value.toString()))
}
