/*
 * Copyright (c) 2025-present http4k Ltd. All rights reserved.
 * Licensed under the http4k Commercial License: https://http4k.org/commercial-license
 */
package org.http4k.lens.patched

import org.http4k.events.Event
import org.http4k.events.MetadataEvent
import org.http4k.events.plus
import org.http4k.lens.int
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class EventMetadataTest {
    private object Happened : Event

    @Test
    fun `value present`() {
        val event = Happened + ("tenant" to "acme")

        assertEquals("acme", EventMetadata.optional("tenant")(event))
        assertEquals("acme", EventMetadata.required("tenant")(event))
        assertEquals(4, EventMetadata.map { it.length }.required("tenant")(event))
    }

    @Test
    fun `value is read as text whatever type it was attached as`() {
        assertEquals(3, EventMetadata.int().required("attempt")(Happened + ("attempt" to 3)))
    }

    @Test
    fun `value missing`() {
        assertEquals(null, EventMetadata.optional("tenant")(Happened))
        assertEquals(null, EventMetadata.optional("tenant")(Happened + ("other" to "x")))
    }

    @Test
    fun `sets value, keeping the event and any other metadata`() {
        val tagged = EventMetadata.required("tenant")("acme", Happened + ("timestamp" to 1))

        assertEquals(MetadataEvent(Happened, mapOf("timestamp" to 1, "tenant" to "acme")), tagged)
    }

    @Test
    fun `setting a value replaces the old one, and setting none removes it`() {
        val lens = EventMetadata.optional("tenant")

        assertEquals(MetadataEvent(Happened, mapOf("tenant" to "globex")), lens("globex", Happened + ("tenant" to "acme")))
        assertEquals(MetadataEvent(Happened, mapOf("timestamp" to 1)), lens(null, Happened + ("tenant" to "acme") + ("timestamp" to 1)))
    }
}
