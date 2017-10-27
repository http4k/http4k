package org.http4k.core

import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.equalTo
import org.junit.Test

class ResponseCacheExtTest {

    @Test
    fun `adds no-cache to response Cache-Control header`() {
        val noCacheResponse = Response(Status.OK).noCache()

        assertThat(noCacheResponse.header("Cache-Control"), equalTo("no-cache"))
    }

    @Test
    fun `adds private to response Cache-Control header`() {
        val maxAgeResponse = Response(Status.OK).private()

        assertThat(maxAgeResponse.header("Cache-Control"), equalTo("private"))
    }

    @Test
    fun `adds public to response Cache-Control header`() {
        val maxAgeResponse = Response(Status.OK).public()

        assertThat(maxAgeResponse.header("Cache-Control"), equalTo("public"))
    }

    @Test
    fun `adds only-if-cached to response Cache-Control header`() {
        val maxAgeResponse = Response(Status.OK).onlyIfCached()

        assertThat(maxAgeResponse.header("Cache-Control"), equalTo("only-if-cached"))
    }

    @Test
    fun `overrides previous value if the header is already set`() {
        val cacheControlAlreadySet = Response(Status.OK).header("Cache-Control", "max-age=100")

        val noCacheResponse = cacheControlAlreadySet.noCache()

        assertThat(noCacheResponse.header("Cache-Control"), equalTo("no-cache"))
    }
}