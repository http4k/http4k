package org.http4k.lens

import com.natpryce.hamkrest.absent
import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.equalTo
import org.http4k.events.Event
import org.http4k.events.MetadataEvent
import org.http4k.events.plus
import org.junit.jupiter.api.Test

class EventMetadataTest {
    private object Happened : Event

    @Test
    fun `value present`() {
        val event = Happened + ("tenant" to "acme")

        assertThat(EventMetadata.optional("tenant")(event), equalTo("acme"))
        assertThat(EventMetadata.required("tenant")(event), equalTo("acme"))
        assertThat(EventMetadata.map { it.length }.required("tenant")(event), equalTo(4))
    }

    @Test
    fun `value is read as text whatever type it was attached as`() {
        assertThat(EventMetadata.int().required("attempt")(Happened + ("attempt" to 3)), equalTo(3))
    }

    @Test
    fun `value missing`() {
        assertThat(EventMetadata.optional("tenant")(Happened), absent())
        assertThat(EventMetadata.optional("tenant")(Happened + ("other" to "x")), absent())
    }

    @Test
    fun `sets value, keeping the event and any other metadata`() {
        val tagged = EventMetadata.required("tenant")("acme", Happened + ("timestamp" to 1))

        assertThat(tagged, equalTo(MetadataEvent(Happened, mapOf("timestamp" to 1, "tenant" to "acme"))))
    }

    @Test
    fun `setting a value replaces the old one, and setting none removes it`() {
        val lens = EventMetadata.optional("tenant")

        assertThat(lens("globex", Happened + ("tenant" to "acme")), equalTo(MetadataEvent(Happened, mapOf("tenant" to "globex"))))
        assertThat(lens(null, Happened + ("tenant" to "acme") + ("timestamp" to 1)), equalTo(MetadataEvent(Happened, mapOf("timestamp" to 1))))
    }
}
