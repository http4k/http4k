package org.http4k.core

import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.equalTo
import org.junit.Test

class ResponseCacheExtTest {

    @Test
    fun `adds public to response Cache-Control header`() {
        val publicResponse = Response(Status.OK).public()

        assertThat(publicResponse.header("Cache-Control"), equalTo("public"))
    }

    @Test
    fun `adds private to response Cache-Control header`() {
        val privateResponse = Response(Status.OK).private()

        assertThat(privateResponse.header("Cache-Control"), equalTo("private"))
    }

    @Test
    fun `adds no-cache to response Cache-Control header`() {
        val noCacheResponse = Response(Status.OK).noCache()

        assertThat(noCacheResponse.header("Cache-Control"), equalTo("no-cache"))
    }

    @Test
    fun `adds only-if-cached to response Cache-Control header`() {
        val onlyIfCachedResponse = Response(Status.OK).onlyIfCached()

        assertThat(onlyIfCachedResponse.header("Cache-Control"), equalTo("only-if-cached"))
    }

    @Test
    fun `adds must-revalidate to response Cache-Control header`() {
        val mustRevalidateResponse = Response(Status.OK).mustRevalidate()

        assertThat(mustRevalidateResponse.header("Cache-Control"), equalTo("must-revalidate"))
    }

    @Test
    fun `adds no-store to response Cache-Control header`() {
        val noStoreResponse = Response(Status.OK).noStore()

        assertThat(noStoreResponse.header("Cache-Control"), equalTo("no-store"))
    }

    @Test
    fun `adds no-transform to response Cache-Control header`() {
        val noTransformResponse = Response(Status.OK).noTransform()

        assertThat(noTransformResponse.header("Cache-Control"), equalTo("no-transform"))
    }

    @Test
    fun `adds proxy-revalidate to response Cache-Control header`() {
        val proxyRevalidateResp = Response(Status.OK).proxyRevalidate()

        assertThat(proxyRevalidateResp.header("Cache-Control"), equalTo("proxy-revalidate"))
    }

    @Test
    fun `adds max-age to response Cache-Control header`() {
        val maxAgeResponse = Response(Status.OK).maxAge(360)

        assertThat(maxAgeResponse.header("Cache-Control"), equalTo("max-age=360"))
    }

    @Test
    fun `adds s-maxage to response Cache-Control header`() {
        val sMaxAgeResponse = Response(Status.OK).sMaxAge(100)

        assertThat(sMaxAgeResponse.header("Cache-Control"), equalTo("s-maxage=100"))
    }

    @Test
    fun `can chain together multiple calls to add to the header`() {
        val chainedResponse = Response(Status.OK).public().maxAge(60)

        assertThat(chainedResponse.header("Cache-Control"), equalTo("public, max-age=60"))
    }
}