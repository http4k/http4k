package org.http4k.websocket

import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.equalTo
import org.junit.jupiter.api.Test

class WsStatusTest {
    @Test
    fun `can override description`() {
        val description = WsStatus.EXTENSION.description("all good")
        assertThat(description.description, equalTo("all good"))
        assertThat(description.toString(), equalTo("1010 all good"))
    }

    @Test
    fun `equality does not include description`() {
        assertThat(WsStatus.EXTENSION.description("foo") == WsStatus.EXTENSION.description("bar"), equalTo(true))
    }
}