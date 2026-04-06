/*
 * Copyright (c) 2025-present http4k Ltd. All rights reserved.
 * Licensed under the http4k Commercial License: https://http4k.org/commercial-license
 */
package org.http4k.verify

import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.equalTo
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.io.File

class VerificationCacheTest {

    @TempDir
    lateinit var gradleHome: File

    private val cache by lazy { VerificationCache(gradleHome) }

    private fun createArtifact(content: String) =
        File.createTempFile("artifact", ".jar").apply {
            deleteOnExit()
            writeText(content)
        }

    @Test
    fun `isVerified returns false for empty cache`() {
        val artifact = createArtifact("hello")

        assertThat(cache.isVerified("test:jar", artifact), equalTo(false))
    }

    @Test
    fun `markVerified then isVerified returns true`() {
        val artifact = createArtifact("hello")

        cache.markVerified("test:jar", artifact)

        assertThat(cache.isVerified("test:jar", artifact), equalTo(true))
    }

    @Test
    fun `different artifact is not verified`() {
        val artifact1 = createArtifact("hello")
        val artifact2 = createArtifact("world")

        cache.markVerified("test:jar", artifact1)

        assertThat(cache.isVerified("test:jar", artifact2), equalTo(false))
    }

    @Test
    fun `does not duplicate entries`() {
        val artifact = createArtifact("hello")

        cache.markVerified("test:jar", artifact)
        cache.markVerified("test:jar", artifact)

        val lines = File(gradleHome, "caches/http4k-verify/verified.txt")
            .readLines()
            .filter { it.isNotBlank() }

        assertThat(lines.size, equalTo(1))
    }
}
