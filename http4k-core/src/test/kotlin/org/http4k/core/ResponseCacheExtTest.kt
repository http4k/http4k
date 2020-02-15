package org.http4k.core

import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.equalTo
import org.http4k.core.Status.Companion.OK
import org.junit.jupiter.api.Test
import java.time.Duration

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
    fun `adds max-age to response Cache-Control header`() {
        val maxAgeResponse = Response(OK).maxAge(Duration.ofMinutes(1))

        assertThat(maxAgeResponse.header("Cache-Control"), equalTo("max-age=60"))
    }

    @Test
    fun `adds stale-while-revalidate to response Cache-Control header`() {
        val maxAgeResponse = Response(OK).staleWhileRevalidate(Duration.ofMinutes(1))

        assertThat(maxAgeResponse.header("Cache-Control"), equalTo("stale-while-revalidate=60"))
    }

    @Test
    fun `adds stale-if-error to response Cache-Control header`() {
        val maxAgeResponse = Response(OK).staleIfError(Duration.ofMinutes(1))

        assertThat(maxAgeResponse.header("Cache-Control"), equalTo("stale-if-error=60"))
    }

    @Test
    fun `can chain together multiple calls to add to the header`() {
        val chainedResponse = Response(OK).public().private().maxAge(Duration.ofMinutes(1)).maxAge(Duration.ofMinutes(2))

        assertThat(chainedResponse.header("Cache-Control"), equalTo("private, max-age=120"))
    }

    @Test
    fun `should overwrite existing headers with new values`() {
        val chainedResponse = Response(OK)
            .public().private()
            .noCache().noCache()
            .noStore().noStore()
            .maxAge(Duration.ofMinutes(1)).maxAge(Duration.ofMinutes(2))
            .staleIfError(Duration.ofMinutes(2)).staleIfError(Duration.ofMinutes(2))

        assertThat(chainedResponse.header("Cache-Control"), equalTo("private, no-cache, no-store, max-age=120, stale-if-error=120"))
    }
}
