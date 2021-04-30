package org.http4k.websocket

import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.equalTo
import org.http4k.websocket.WsStatus.Companion.EXTENSION
import org.junit.jupiter.api.Test

class WsStatusTest {
    @Test
    fun `can override description`() {
        val description = EXTENSION.description("all good")
        assertThat(description.description, equalTo("all good"))
        assertThat(description.toString(), equalTo("1010 all good"))
    }

    @Test
    fun `equality does not include description`() {
        assertThat(EXTENSION.description("foo") == EXTENSION.description("bar"), equalTo(true))
    }
}
