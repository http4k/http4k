package org.http4k.events

import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.equalTo
import org.junit.jupiter.api.Test

class EventTest {
    @Test
    fun `combines into a singular event with metadata`() {
        val final = MyEvent() + ("first" to "1") + ("second" to 2)
        assertThat(final, equalTo(MetadataEvent(MyEvent(), mapOf("first" to "1", "second" to 2))))
    }
}
