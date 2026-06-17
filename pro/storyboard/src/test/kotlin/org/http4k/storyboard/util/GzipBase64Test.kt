/*
 * Copyright (c) 2025-present http4k Ltd. All rights reserved.
 * Licensed under the http4k Commercial License: https://http4k.org/commercial-license
 */
package org.http4k.storyboard.util

import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.equalTo
import org.junit.jupiter.api.Test

class GzipBase64Test {

    @Test
    fun `round trips a typical HTML payload`() {
        val original = "<html><body><h1>hello</h1>${"x".repeat(2_000)}</body></html>"

        assertThat(original.gzipBase64Encode().gzipBase64Decode(), equalTo(original))
    }

    @Test
    fun `empty string round trips to empty`() {
        assertThat("".gzipBase64Encode(), equalTo(""))
        assertThat("".gzipBase64Decode(), equalTo(""))
    }

    @Test
    fun `compresses repetitive text smaller than plain base64`() {
        val original = "<p>repeat me </p>".repeat(500)

        val plainBase64Size = java.util.Base64.getEncoder().encodeToString(original.toByteArray()).length
        val gzipBase64Size = original.gzipBase64Encode().length

        assertThat(gzipBase64Size < plainBase64Size / 4, equalTo(true))
    }
}
