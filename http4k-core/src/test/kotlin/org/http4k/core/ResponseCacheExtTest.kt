package org.http4k.core

import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.equalTo
import org.http4k.core.Status.Companion.OK
import org.junit.jupiter.api.Test
import java.time.Duration
import kotlin.time.Duration.Companion.hours
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds

class ResponseCacheExtTest {

    @Test
    fun `adds public to response Cache-Control header`() {
        val publicResponse = Response(OK).public()

        assertThat(publicResponse.header("Cache-Control"), equalTo("public"))
    }

    @Test
    fun `adds private to response Cache-Control header`() {
        val privateResponse = Response(OK).private()

        assertThat(privateResponse.header("Cache-Control"), equalTo("private"))
    }

    @Test
    fun `adds no-cache to response Cache-Control header`() {
        val noCacheResponse = Response(OK).noCache()

        assertThat(noCacheResponse.header("Cache-Control"), equalTo("no-cache"))
    }

    @Test
    fun `adds only-if-cached to response Cache-Control header`() {
        val onlyIfCachedResponse = Response(OK).onlyIfCached()

        assertThat(onlyIfCachedResponse.header("Cache-Control"), equalTo("only-if-cached"))
    }

    @Test
    fun `adds must-revalidate to response Cache-Control header`() {
        val mustRevalidateResponse = Response(OK).mustRevalidate()

        assertThat(mustRevalidateResponse.header("Cache-Control"), equalTo("must-revalidate"))
    }

    @Test
    fun `adds no-store to response Cache-Control header`() {
        val noStoreResponse = Response(OK).noStore()

        assertThat(noStoreResponse.header("Cache-Control"), equalTo("no-store"))
    }

    @Test
    fun `adds immutable to a response Cache-Control header`() {
        val immutableResponse = Response(OK).immutable()

        assertThat(immutableResponse.header("Cache-Control"), equalTo("immutable"))
    }

    @Test
    fun `retrieves null max age if no Cache-Control header`() {
        val response = Response(OK)
        assertThat(response.maxAge(), equalTo(null))
    }

    @Test
    fun `retrieves null max age if Cache-Control header has no max-age directive`() {
        val response = Response(OK).header("Cache-Control", "public")
        assertThat(response.maxAge(), equalTo(null))
    }

    @Test
    fun `retrieves null max age if Cache-Control header has invalid max-age directive`() {
        val response = Response(OK).header("Cache-Control", "max-age=not_a_number")
        assertThat(response.maxAge(), equalTo(null))
    }

    @Test
    fun `retrieves max age from Cache-Control header if valid`() {
        val response = Response(OK).header("Cache-Control", "public, max-age=60, stale-if-error=1000")
        assertThat(response.maxAge(), equalTo(60))
    }

    @Test
    fun `adds max-age to response Cache-Control header from Java duration`() {
        val maxAgeResponse = Response(OK).maxAge(Duration.ofMinutes(1))

        assertThat(maxAgeResponse.header("Cache-Control"), equalTo("max-age=60"))
    }

    @Test
    fun `adds max-age to response Cache-Control header from Kotlin duration`() {
        val maxAgeResponse = Response(OK).maxAge(2.hours)

        assertThat(maxAgeResponse.header("Cache-Control"), equalTo("max-age=7200"))
    }

    @Test
    fun `retrieves null stale-while-revalidate if no Cache-Control header`() {
        val response = Response(OK)
        assertThat(response.staleWhileRevalidate(), equalTo(null))
    }

    @Test
    fun `retrieves null stale-while-revalidate if Cache-Control header has no directive`() {
        val response = Response(OK).header("Cache-Control", "public")
        assertThat(response.staleWhileRevalidate(), equalTo(null))
    }

    @Test
    fun `retrieves null stale-while-revalidate if Cache-Control header has invalid directive`() {
        val response = Response(OK).header("Cache-Control", "stale-while-revalidate=not_a_number")
        assertThat(response.staleWhileRevalidate(), equalTo(null))
    }

    @Test
    fun `retrieves stale-while-revalidate from Cache-Control header if valid`() {
        val response = Response(OK).header("Cache-Control", "public, max-age=60, stale-while-revalidate=1000")
        assertThat(response.staleWhileRevalidate(), equalTo(1000))
    }

    @Test
    fun `adds stale-while-revalidate to response Cache-Control header from Java duration`() {
        val maxAgeResponse = Response(OK).staleWhileRevalidate(Duration.ofMinutes(1))

        assertThat(maxAgeResponse.header("Cache-Control"), equalTo("stale-while-revalidate=60"))
    }

    @Test
    fun `adds stale-while-revalidate to response Cache-Control header from Kotlin Duration`() {
        val maxAgeResponse = Response(OK).staleWhileRevalidate(3.minutes)

        assertThat(maxAgeResponse.header("Cache-Control"), equalTo("stale-while-revalidate=180"))
    }

    @Test
    fun `retrieves null stale-if-error if no Cache-Control header`() {
        val response = Response(OK)
        assertThat(response.staleIfError(), equalTo(null))
    }

    @Test
    fun `retrieves null stale-if-error if Cache-Control header has no directive`() {
        val response = Response(OK).header("Cache-Control", "public")
        assertThat(response.staleIfError(), equalTo(null))
    }

    @Test
    fun `retrieves null stale-if-error if Cache-Control header has invalid directive`() {
        val response = Response(OK).header("Cache-Control", "stale-if-error=not_a_number")
        assertThat(response.staleIfError(), equalTo(null))
    }

    @Test
    fun `retrieves stale-if-error from Cache-Control header if valid`() {
        val response = Response(OK).header("Cache-Control", "stale-if-error=10, public, max-age=60")
        assertThat(response.staleIfError(), equalTo(10))
    }

    @Test
    fun `adds stale-if-error to response Cache-Control header from Java duration`() {
        val maxAgeResponse = Response(OK).staleIfError(Duration.ofMinutes(1))

        assertThat(maxAgeResponse.header("Cache-Control"), equalTo("stale-if-error=60"))
    }

    @Test
    fun `adds stale-if-error to response Cache-Control header from Kotlin Duration`() {
        val maxAgeResponse = Response(OK).staleIfError(6.seconds)

        assertThat(maxAgeResponse.header("Cache-Control"), equalTo("stale-if-error=6"))
    }

    @Test
    fun `can chain together multiple calls to add to the header`() {
        val chainedResponse = Response(OK).public().private().maxAge(Duration.ofMinutes(1)).maxAge(Duration.ofMinutes(2)).immutable()

        assertThat(chainedResponse.header("Cache-Control"), equalTo("private, max-age=120, immutable"))
    }

    @Test
    fun `should overwrite existing headers with new values`() {
        val chainedResponse = Response(OK)
            .public().private()
            .noCache().noCache()
            .noStore().noStore()
            .immutable().immutable()
            .maxAge(Duration.ofMinutes(1)).maxAge(Duration.ofMinutes(2))
            .staleIfError(Duration.ofMinutes(2)).staleIfError(Duration.ofMinutes(2))

        assertThat(chainedResponse.header("Cache-Control"), equalTo("private, no-cache, no-store, immutable, max-age=120, stale-if-error=120"))
    }
}
