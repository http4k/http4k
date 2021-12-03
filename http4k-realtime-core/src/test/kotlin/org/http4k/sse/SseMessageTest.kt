package org.http4k.sse

import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.equalTo
import org.http4k.base64Encode
import org.junit.jupiter.api.Test

class SseMessageTest {

    @Test
    fun `encodes binary as base 64`() {
        assertThat(SseMessage.Data("body".byteInputStream()).data, equalTo("body".base64Encode()))
    }
}
