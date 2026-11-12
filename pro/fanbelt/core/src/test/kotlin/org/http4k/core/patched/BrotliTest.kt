/*
 * Copyright (c) 2025-present http4k Ltd. All rights reserved.
 * Licensed under the http4k Commercial License: https://http4k.org/commercial-license
 */
package org.http4k.core.patched

import org.http4k.core.ContentEncoding
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.io.ByteArrayOutputStream
import kotlin.random.Random

class BrotliTest {
    private val block = Random(1).nextBytes(100_000)
    private val repeatedPastAGap = block + Random(2).nextBytes(100_000) + block

    private fun encodedSize(encoding: ContentEncoding) =
        ByteArrayOutputStream().also { out -> encoding.encoder(out).use { it.write(repeatedPastAGap) } }.size()

    @Test
    fun `a repeat further back than the window is not found`() {
        val narrow = encodedSize(ContentEncoding.Brotli(window = 16))
        val wide = encodedSize(ContentEncoding.Brotli(window = 22))

        assertTrue(narrow > wide + 90_000, "window 2^16 encoded to $narrow bytes, 2^22 to $wide")
    }
}
