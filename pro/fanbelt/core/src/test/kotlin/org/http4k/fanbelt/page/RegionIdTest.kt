/*
 * Copyright (c) 2025-present http4k Ltd. All rights reserved.
 * Licensed under the http4k Commercial License: https://http4k.org/commercial-license
 */
package org.http4k.fanbelt.page

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

class RegionIdTest {
    @Test
    fun `a region id is a letter then letters, digits, - or _`() {
        assertEquals("row-1_a", RegionId.of("row-1_a").value)
    }

    @Test
    fun `anything else is refused, so a region can't be built with it`() {
        listOf("", "1row", "-row", "a b", "a\"b").forEach { bad ->
            assertThrows<IllegalArgumentException>(bad) { RegionId.of(bad) }
        }
    }
}
