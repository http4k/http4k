package org.http4k.filter

import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.equalTo
import com.natpryce.hamkrest.present
import org.http4k.client.JavaHttpClient
import org.http4k.core.Method.GET
import org.http4k.core.Method.POST
import org.http4k.core.Request
import org.http4k.core.Response
import org.http4k.core.Status
import org.http4k.core.Status.Companion.OK
import org.http4k.core.Uri
import org.http4k.core.maxAge
import org.http4k.core.staleIfError
import org.http4k.core.staleWhileRevalidate
import org.http4k.core.then
import org.http4k.filter.TrafficFilters.CacheExpiryParams
import org.http4k.filter.TrafficFilters.RecordTo
import org.http4k.routing.path
import org.http4k.server.SunHttp
import org.http4k.server.asServer
import org.http4k.traffic.ReadWriteCache
import org.http4k.traffic.ReadWriteStream
import org.http4k.util.PortBasedTest
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.fail
import java.util.concurrent.CompletableFuture
import kotlin.time.Duration.Companion.days
import kotlin.time.Duration.Companion.hours
import kotlin.time.Duration.Companion.seconds
import kotlin.time.Instant

class TrafficFiltersTest : PortBasedTest {
    private val request = Request(GET, "/bob")
    private val response = Response(OK)

    @Test
    fun `RecordTo stores traffic in underlying storage`() {
        val stream = ReadWriteStream.Memory()

        val handler = RecordTo(stream).then { response }

        assertThat(handler(request), equalTo(response))

        assertThat(stream.requests().toList(), equalTo(listOf(request)))
        assertThat(stream.responses().toList(), equalTo(listOf(response)))
    }

    @Test
    fun `RecordTo stores traffic in underlying storage on server`() {
        val stream = ReadWriteStream.Memory()

        val request1 = Request(POST, "").body("helloworld")
        val request2 = Request(POST, "").body("goodbyeworld")

        RecordTo(stream)
            .then { responseFor(it) }
            .asServer(SunHttp(0))
            .start().use {
                val client = ClientFilters.SetBaseUriFrom(Uri.of("http://localhost:${it.port()}"))
                    .then(JavaHttpClient())
                assertThat(client(request1), org.http4k.hamkrest.hasBody("helloworld".reversed()))
                assertThat(client(request2), org.http4k.hamkrest.hasBody("goodbyeworld".reversed()))
            }

        assertThat(stream.requests().toList()[0], org.http4k.hamkrest.hasBody(request1.bodyString()))
        assertThat(stream.requests().toList()[1], org.http4k.hamkrest.hasBody(request2.bodyString()))
        assertThat(stream.responses().toList()[0], org.http4k.hamkrest.hasBody(responseFor(request1).bodyString()))
        assertThat(stream.responses().toList()[1], org.http4k.hamkrest.hasBody(responseFor(request2).bodyString()))
    }

    private fun responseFor(req: Request) =
        Response(OK).body(req.body.stream.reader().readText().reversed().byteInputStream())

    @Test
    fun `ServeCachedFrom serves stored requests later or falls back`() {
        val cache = ReadWriteCache.Memory()
        cache[request] = response
        val notFound = Response(Status.NOT_FOUND)
        val handler = TrafficFilters.ServeCachedFrom(cache).then { notFound }

        assertThat(handler(request), equalTo(response))
        assertThat(handler(Request(GET, "/bob2")), equalTo(notFound))
    }

    @Test
    fun `ReplayFrom serves stored requests later or returns 400`() {
        val cache = ReadWriteStream.Memory()
        cache[Request(GET, "/bob1")] = Response(OK)
        cache[Request(GET, "/bob2")] = Response(Status.ACCEPTED)
        cache[Request(GET, "/bob3")] = Response(Status.NOT_FOUND)
        val handler = TrafficFilters.ReplayFrom(cache).then { fail("") }

        assertThat(handler(Request(GET, "/bob1")), equalTo(Response(OK)))
        assertThat(handler(Request(GET, "/bob2")), equalTo(Response(Status.ACCEPTED)))
        assertThat(handler(Request(GET, "/bob3")), equalTo(Response(Status.NOT_FOUND)))
        assertThat(handler(Request(GET, "/bob2")), equalTo(Response(Status.BAD_REQUEST)))
    }

    @Test
    fun `RecordToAndServeFromCache combines both RecordTo and ServeCachedFrom`() {
        val cache = ReadWriteCache.Memory()

        var requests = 0
        val handler = TrafficFilters.RecordToAndServeFromCache(cache).then {
            requests++
            response
        }

        assertThat(handler(request), equalTo(response))
        assertThat(requests, equalTo(1))

        assertThat(handler(request), equalTo(response))
        assertThat(requests, equalTo(1))

        assertThat(handler(request.uri(Uri.of("/other"))), equalTo(response))
        assertThat(requests, equalTo(2))
    }

    @Nested
    inner class CacheExpiryParamsTest {

        @Test
        fun `when null CacheExpiryParams is provided, RecordTo stores the response without any expiry headers`() {
            val stream = ReadWriteStream.Memory()
            val maxAgeResponse = response.maxAge(30.seconds)

            val handler = RecordTo(stream).then { maxAgeResponse }

            assertThat(handler(request), equalTo(maxAgeResponse))

            assertThat(stream.responses().toList(), equalTo(listOf(maxAgeResponse)))
        }

        @Test
        fun `when non-null CacheExpiryParams is provided, RecordTo stores the response with the configured expiry headers but the untransformed response is returned`() {
            val stream = ReadWriteStream.Memory()
            val maxAgeResponse = response.maxAge(30.seconds)

            val handler = RecordTo(
                stream,
                CacheExpiryParams(staleWhileRevalidate = null, staleIfError = null),
                { Instant.fromEpochMilliseconds(123456) }
            ).then { maxAgeResponse }

            assertThat(handler(request), equalTo(maxAgeResponse))

            val expectedStoredResponse = maxAgeResponse.header("x-http4k-expiry", "${123456 + 30_000}")
            assertThat(stream.responses().toList(), equalTo(listOf(expectedStoredResponse)))
        }

        @Test
        fun `when non-null CacheExpiryParams is provided but response has no max-age RecordTo stores the response with an expiry of now`() {
            val stream = ReadWriteStream.Memory()

            val handler = RecordTo(
                stream,
                CacheExpiryParams(staleWhileRevalidate = null, staleIfError = null),
                { Instant.fromEpochMilliseconds(123456) }
            ).then { response }

            assertThat(handler(request), equalTo(response))

            val expectedStoredResponse = response.header("x-http4k-expiry", "123456")
            assertThat(stream.responses().toList(), equalTo(listOf(expectedStoredResponse)))
        }

        @Test
        fun `when a full set of CacheExpiryParams is provided and response has all the cache control directives, RecordTo stores all the cache expiry headers`() {
            val stream = ReadWriteStream.Memory()
            val cacheControlResponse = response
                .maxAge(25.seconds)
                .staleWhileRevalidate(2.hours)
                .staleIfError(1.days)

            val handler = RecordTo(
                stream,
                CacheExpiryParams(),
                { Instant.fromEpochMilliseconds(123456) }
            ).then { cacheControlResponse }

            assertThat(handler(request), equalTo(cacheControlResponse))

            val expectedStoredResponse = cacheControlResponse
                .header("x-http4k-expiry", "${123456 + 25_000}")
                .header("x-http4k-stale-while-revalidate", "${123456 + 25_000 + 7_200_000}")
                .header("x-http4k-stale-if-error", "${123456 + 25_000 + 8_6400_000}")
            assertThat(stream.responses().toList(), equalTo(listOf(expectedStoredResponse)))
        }

        @Test
        fun `when the response has an age header, RecordTo deducts this from the expiry time`() {
            val stream = ReadWriteStream.Memory()
            val cacheControlResponse = response
                .header("Age", "10")
                .maxAge(25.seconds)
                .staleWhileRevalidate(2.hours)
                .staleIfError(1.days)

            val handler = RecordTo(
                stream,
                CacheExpiryParams(),
                { Instant.fromEpochMilliseconds(123456) }
            ).then { cacheControlResponse }

            assertThat(handler(request), equalTo(cacheControlResponse))

            val expectedStoredResponse = cacheControlResponse
                .header("x-http4k-expiry", "${123456 + 15_000}")
                .header("x-http4k-stale-while-revalidate", "${123456 + 15_000 + 7_200_000}")
                .header("x-http4k-stale-if-error", "${123456 + 15_000 + 8_6400_000}")
            assertThat(stream.responses().toList(), equalTo(listOf(expectedStoredResponse)))
        }

        @Test
        fun `when the response has an invalid age header, RecordTo ignores it`() {
            val stream = ReadWriteStream.Memory()
            val cacheControlResponse = response
                .header("Age", "NaN")
                .maxAge(25.seconds)
                .staleWhileRevalidate(2.hours)
                .staleIfError(1.days)

            val handler = RecordTo(
                stream,
                CacheExpiryParams(),
                { Instant.fromEpochMilliseconds(123456) }
            ).then { cacheControlResponse }

            assertThat(handler(request), equalTo(cacheControlResponse))

            val expectedStoredResponse = cacheControlResponse
                .header("x-http4k-expiry", "${123456 + 25_000}")
                .header("x-http4k-stale-while-revalidate", "${123456 + 25_000 + 7_200_000}")
                .header("x-http4k-stale-if-error", "${123456 + 25_000 + 8_6400_000}")
            assertThat(stream.responses().toList(), equalTo(listOf(expectedStoredResponse)))
        }

        @Test
        fun `when non-null CacheExpiryParams is provided, ServeCachedFrom serves requests from the cache if they are within the expiry, minus the custom headers`() {
            val cache = ReadWriteCache.Memory()
            cache[request] = response.header("x-custom-expiry-header", "1234")

            val notFound = Response(Status.NOT_FOUND)
            val handler = TrafficFilters.ServeCachedFrom(
                cache,
                CacheExpiryParams(expiry = "x-custom-expiry-header"),
                { Instant.fromEpochMilliseconds(1233) }
            ).then { notFound }

            assertThat(handler(request), equalTo(response))
        }

        @Test
        fun `when non-null CacheExpiryParams is provided, ServeCachedFrom falls back to the next if the response has expired`() {
            val cache = ReadWriteCache.Memory()
            cache[request] = response.header("x-custom-expiry-header", "1234")

            val notFound = Response(Status.NOT_FOUND)
            val handler = TrafficFilters.ServeCachedFrom(
                cache,
                CacheExpiryParams(expiry = "x-custom-expiry-header"),
                { Instant.fromEpochMilliseconds(1234) }
            ).then { notFound }

            assertThat(handler(request), equalTo(notFound))
        }

        @Test
        fun `when non-null CacheExpiryParams with staleWhileRevalidate is provided, ServeCachedFrom will return a non fresh response while inside staleWhileRevalidate expiry while revalidating`() {
            val cache = ReadWriteCache.Memory()
            cache[request] = response
                .header("x-custom-expiry-header", "111")
                .header("x-custom-stale-while-revalidate-header", "333")

            val revalidateJob = CompletableFuture<Runnable?>()

            val notFound = Response(Status.NOT_FOUND)
            val handler = TrafficFilters.ServeCachedFrom(
                cache,
                CacheExpiryParams(
                    expiry = "x-custom-expiry-header",
                    staleWhileRevalidate = "x-custom-stale-while-revalidate-header",
                    launchRevalidateTask = { revalidateJob.complete(it) }
                ),
                { Instant.fromEpochMilliseconds(222) }
            ).then { notFound }

            assertThat(handler(request), equalTo(response))
            assertThat(revalidateJob.get(), present())
        }

        @Test
        fun `when non-null CacheExpiryParams with staleWhileRevalidate is provided, ServeCachedFrom will fall back to the next if response is too stale for the staleWhileRevalidate expiry`() {
            val cache = ReadWriteCache.Memory()
            cache[request] = response
                .header("x-custom-expiry-header", "111")
                .header("x-custom-stale-while-revalidate-header", "222")


            val notFound = Response(Status.NOT_FOUND)
            val handler = TrafficFilters.ServeCachedFrom(
                cache,
                CacheExpiryParams(
                    expiry = "x-custom-expiry-header",
                    staleWhileRevalidate = "x-custom-stale-while-revalidate-header",
                ),
                { Instant.fromEpochMilliseconds(333) }
            ).then { notFound }

            assertThat(handler(request), equalTo(notFound))
        }


        @Test
        fun `when non-null CacheExpiryParams with staleIfError is provided, ServeCachedFrom will serve a stale cached response if within the staleIfError expiry and next returns a server error`() {
            val cache = ReadWriteCache.Memory()
            cache[request] = response
                .header("x-custom-expiry-header", "111")
                .header("x-custom-stale-while-revalidate-header", "222")
                .header("x-custom-stale-if-error-header", "444")

            val serverError = Response(Status.INTERNAL_SERVER_ERROR)
            val handler = TrafficFilters.ServeCachedFrom(
                cache,
                CacheExpiryParams(
                    expiry = "x-custom-expiry-header",
                    staleWhileRevalidate = "x-custom-stale-while-revalidate-header",
                    staleIfError = "x-custom-stale-if-error-header"
                ),
                { Instant.fromEpochMilliseconds(333) }
            ).then { serverError }

            assertThat(handler(request), equalTo(response))
        }

        @Test
        fun `when non-null CacheExpiryParams with staleIfError is provided, ServeCachedFrom will not serve a stale cached response if within the staleIfError expiry but the fetched response is not a server error`() {
            val cache = ReadWriteCache.Memory()
            cache[request] = response
                .header("x-custom-expiry-header", "111")
                .header("x-custom-stale-while-revalidate-header", "222")
                .header("x-custom-stale-if-error-header", "444")

            val accepted = Response(Status.ACCEPTED)
            val handler = TrafficFilters.ServeCachedFrom(
                cache,
                CacheExpiryParams(
                    expiry = "x-custom-expiry-header",
                    staleWhileRevalidate = "x-custom-stale-while-revalidate-header",
                    staleIfError = "x-custom-stale-if-error-header"
                ),
                { Instant.fromEpochMilliseconds(333) }
            ).then { accepted }

            assertThat(handler(request), equalTo(accepted))
        }

        @Test
        fun `when non-null CacheExpiryParams with staleIfError is provided, ServeCachedFrom will serve the fetched error response if outside of the staleIfError expiry`() {
            val cache = ReadWriteCache.Memory()
            cache[request] = response
                .header("x-custom-expiry-header", "111")
                .header("x-custom-stale-while-revalidate-header", "222")
                .header("x-custom-stale-if-error-header", "333")

            val serverError = Response(Status.INTERNAL_SERVER_ERROR)
            val handler = TrafficFilters.ServeCachedFrom(
                cache,
                CacheExpiryParams(
                    expiry = "x-custom-expiry-header",
                    staleWhileRevalidate = "x-custom-stale-while-revalidate-header",
                    staleIfError = "x-custom-stale-if-error-header"
                ),
                { Instant.fromEpochMilliseconds(444) }
            ).then { serverError }

            assertThat(handler(request), equalTo(serverError))
        }

        @Test
        fun `when an invalid expiry value is set, this is ignored and the response is considered stale`() {
            val cache = ReadWriteCache.Memory()
            cache[request] = response.header("x-custom-expiry-header", "Invalid")

            val serverError = Response(Status.INTERNAL_SERVER_ERROR)
            val handler = TrafficFilters.ServeCachedFrom(
                cache,
                CacheExpiryParams(
                    expiry = "x-custom-expiry-header",
                    staleWhileRevalidate = null,
                    staleIfError = null,
                ),
                { Instant.fromEpochMilliseconds(123) }
            ).then { serverError }

            assertThat(handler(request), equalTo(serverError))
        }
    }
}
