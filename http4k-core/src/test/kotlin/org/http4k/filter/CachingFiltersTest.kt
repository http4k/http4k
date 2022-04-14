package org.http4k.filter

import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.equalTo
import org.http4k.core.Filter
import org.http4k.core.Method.GET
import org.http4k.core.Method.PUT
import org.http4k.core.Request
import org.http4k.core.Response
import org.http4k.core.Status.Companion.OK
import org.http4k.core.then
import org.http4k.filter.CachingFilters.Request.AddIfModifiedSince
import org.http4k.filter.CachingFilters.Response.AddETag
import org.http4k.filter.CachingFilters.Response.FallbackCacheControl
import org.http4k.filter.CachingFilters.Response.MaxAge
import org.http4k.filter.CachingFilters.Response.NoCache
import org.http4k.hamkrest.hasHeader
import org.http4k.util.FixedClock
import org.junit.jupiter.api.Test
import java.time.Duration
import java.time.Duration.ZERO
import java.time.Duration.ofSeconds
import java.time.ZonedDateTime
import java.time.format.DateTimeFormatter.RFC_1123_DATE_TIME

class CachingFiltersTest {

    private val clock = FixedClock
    private val maxAge = ofSeconds(10)
    private val timings = DefaultCacheTimings(MaxAgeTtl(maxAge), StaleIfErrorTtl(ofSeconds(2000)), StaleWhenRevalidateTtl(ofSeconds(3000)))
    private val timingsWithZeroValues = DefaultCacheTimings(MaxAgeTtl(maxAge), StaleIfErrorTtl(ZERO), StaleWhenRevalidateTtl(ZERO))

    private val request = org.http4k.core.Request(GET, "")
    private val response = Response(OK)

    @Test
    fun `Adds If-Modified-Since to Request`() {
        val maxAge = Duration.ofSeconds(1)
        val response = AddIfModifiedSince(clock, maxAge).then { Response(OK).header("If-modified-since", it.header("If-modified-since")) }(
            request)
        assertThat(response, hasHeader("If-modified-since", RFC_1123_DATE_TIME.format(ZonedDateTime.now(clock).minus(maxAge))))
    }

    @Test
    fun `Add eTag`() {
        val response = AddETag { true }.then { Response(OK).body("bob") }(
            request)
        assertThat(response, hasHeader("etag", "9f9d51bc70ef21ca5c14f307980a29d8"))
    }

    private fun getResponseWith(cacheTimings: DefaultCacheTimings, response: Response) = FallbackCacheControl(clock, cacheTimings).then { response }(request)

    @Test
    fun `FallbackCacheControl - adds the headers if they are not set`() {

        val responseWithNoHeaders = Response(OK)
        val response = getResponseWith(timings, responseWithNoHeaders)

        assertThat(response, hasHeader("Cache-Control", "public, max-age=10, stale-while-revalidate=3000, stale-if-error=2000"))
        assertThat(response, hasHeader("Expires", RFC_1123_DATE_TIME.format(ZonedDateTime.now(clock).plus(maxAge))))
        assertThat(response, hasHeader("Vary", "Accept-Encoding"))
    }

    @Test
    fun `FallbackCacheControl - does not overwrite the headers if they are set`() {
        val responseWithHeaders = Response(OK).header("Cache-Control", "rita").header("Expires", "sue").header("Vary", "bob")
        val response = getResponseWith(timings, responseWithHeaders)

        assertThat(response, hasHeader("Cache-Control", listOf("rita")))
        assertThat(response, hasHeader("Expires", listOf("sue")))
        assertThat(response, hasHeader("Vary", listOf("bob")))
    }

    @Test
    fun `FallbackCacheControl - renders cache header correctly if some directives have an empty Duration`() {
        val responseWithHeaders = Response(OK)
        val response = getResponseWith(timingsWithZeroValues, responseWithHeaders)

        assertThat(response, hasHeader("Cache-Control", listOf("public, max-age=10")))
    }

    @Test
    fun `NoCache - does not cache non-GET requests`() {
        val response = NoCache().responseFor(Request(PUT, ""))
        assertThat(response.headers, equalTo(emptyList()))
    }

    @Test
    fun `NoCache - adds correct headers to GET responses`() {
        val response = NoCache().responseFor(Request(GET, ""))
        assertThat(response, hasHeader("Cache-Control", "private, must-revalidate"))
        assertThat(response, hasHeader("Expires", "0"))
    }

    @Test
    fun `NoCache - does not add headers if response fails predicate`() {
        val response = NoCache { false }.responseFor(Request(GET, ""))
        assertThat(response.headers, equalTo(emptyList()))
    }

    @Test
    fun `MaxAge - does not cache non-GET requests`() {
        val response = MaxAge(clock, Duration.ofHours(1)).responseFor(Request(PUT, ""))
        assertThat(response.headers, equalTo(emptyList()))
    }

    @Test
    fun `MaxAge - adds correct headers to GET responses`() {
        val response = MaxAge(clock, Duration.ofHours(1)).responseFor(Request(GET, ""))
        assertThat(response, hasHeader("Cache-Control", "public, max-age=3600"))
        assertThat(response, hasHeader("Expires", ZonedDateTime.now(clock).plusHours(1).format(RFC_1123_DATE_TIME)))
    }

    @Test
    fun `MaxAge - adds correct headers to GET when illegal header value`() {
        val responseWithHeaders = Response(OK).header("Date", "foobar")

        val response = (MaxAge(clock, Duration.ofHours(1)).then { responseWithHeaders })(Request(GET, ""))
        assertThat(response, hasHeader("Cache-Control", "public, max-age=3600"))
        assertThat(response, hasHeader("Expires", ZonedDateTime.now(clock).plusHours(1).format(RFC_1123_DATE_TIME)))
    }

    @Test
    fun `MaxAge - does not add headers if response fails predicate`() {
        val response = MaxAge(clock, Duration.ofHours(1)) { false }.responseFor(Request(GET, ""))
        assertThat(response.headers, equalTo(emptyList()))
    }

    private fun Filter.responseFor(request: Request) = then { response }(request)
}
