package org.http4k.filter

import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.equalTo
import org.http4k.core.Method
import org.http4k.core.Method.*
import org.http4k.core.Request
import org.http4k.core.Response
import org.http4k.core.Status
import org.http4k.core.Status.Companion.OK
import org.http4k.core.cookie.Cookie
import org.http4k.core.cookie.cookie
import org.http4k.core.then
import org.http4k.filter.cookie.BasicCookieStorage
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
            val response = client(Request(GET, "/"))
            assertThat(response, hasHeader("Set-Cookie", """counter="${it + 1}"; """))
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

        val cookieStorage = BasicCookieStorage()

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

        assertThat(cookieStorage.retrieve().size, equalTo(1))

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

        val cookieStorage = BasicCookieStorage()

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

        assertThat(cookieStorage.retrieve().size, equalTo(1))

        // if the parser uses UTC and the expiry checker uses local time then this will be 'gone'
        assertThat(client(Request(GET, "/get")), hasBody("bar"))

        clock.add(1)

        // if the parser uses local time and the expiry checker uses UTC then this will be 'bar'
        assertThat(client(Request(GET, "/get")), hasBody("gone"))
    }

    fun Request.counterCookie() = cookie("counter")?.value?.toInt() ?: 0
    fun Response.counterCookie(value: Int) = cookie(Cookie("counter", value.toString()))
}
