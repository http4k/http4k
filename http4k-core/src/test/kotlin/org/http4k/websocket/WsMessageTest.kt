package org.http4k.websocket

import com.natpryce.hamkrest.equalTo
import com.natpryce.hamkrest.should.shouldMatch
import org.http4k.core.Body
import org.http4k.core.MemoryBody
import org.http4k.core.StreamBody
import org.junit.Test

class WsMessageTest {

    @Test
    fun `calls through to create correct body`() {
        val memoryBody = MemoryBody("body")
        WsMessage("body").body shouldMatch equalTo(memoryBody as Body)
        WsMessage(memoryBody).body shouldMatch equalTo(memoryBody as Body)
        WsMessage("hello".byteInputStream()).body shouldMatch equalTo(StreamBody("hello".byteInputStream(), 5) as Body)
    }
}