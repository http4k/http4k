package org.http4k.websocket

import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.equalTo
import org.http4k.core.Body
import org.http4k.core.MemoryBody
import org.http4k.core.StreamBody
import org.junit.jupiter.api.Test

class WsMessageTest {

    @Test
    fun `calls through to create correct body`() {
        val memoryBody = MemoryBody("body")
        assertThat(WsMessage("body").body, equalTo(memoryBody as Body))
        assertThat(WsMessage(memoryBody).body, equalTo(memoryBody as Body))
        assertThat(WsMessage("hello".byteInputStream()).body, equalTo(StreamBody("hello".byteInputStream(), 5) as Body))
    }
}
