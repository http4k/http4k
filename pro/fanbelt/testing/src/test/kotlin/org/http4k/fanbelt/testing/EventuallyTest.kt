/*
 * Copyright (c) 2025-present http4k Ltd. All rights reserved.
 * Licensed under the http4k Commercial License: https://http4k.org/commercial-license
 */
package org.http4k.fanbelt.testing

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows
import java.time.Duration
import java.util.concurrent.atomic.AtomicInteger

class EventuallyTest {
    @Test
    fun `a list that hasn't filled yet is worth waiting for`() {
        val reads = AtomicInteger()

        val found = eventually { List(reads.getAndIncrement()) { "row" }.single() }

        assertEquals("row", found)
    }

    @Test
    fun `an assertion that doesn't hold yet is worth waiting for`() {
        val reads = AtomicInteger()

        eventually { assertEquals(3, reads.incrementAndGet()) }

        assertEquals(3, reads.get())
    }

    @Test
    fun `an unexpected failure is not waited on`() {
        val reads = AtomicInteger()

        assertThrows<IllegalStateException> {
            eventually<Unit> {
                reads.incrementAndGet()
                error("broken")
            }
        }
        assertEquals(1, reads.get())
    }

    @Test
    fun `it gives up in the end`() {
        assertThrows<NoSuchElementException> { eventually(Duration.ofMillis(50)) { emptyList<String>().single() } }
    }
}
