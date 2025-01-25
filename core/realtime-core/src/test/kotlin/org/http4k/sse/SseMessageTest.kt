package org.http4k.sse

import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.equalTo
import org.http4k.base64Encode
import org.junit.jupiter.api.Test
import java.time.Duration

class SseMessageTest {

    @Test
    fun `encodes binary as base 64`() {
        assertThat(SseMessage.Data("body".byteInputStream()).data, equalTo("body".base64Encode()))
    }

    @Test
    fun `converts data toMessage`() {
        assertRoundtrip(SseMessage.Data("body"), "data: body\n\n")
    }

    @Test
    fun `converts event toMessage`() {
        assertRoundtrip(
            SseMessage.Event("event", "data1\ndata2", "id"), """event: event
data: data1
data: data2
id: id

"""
        )
    }

    @Test
    fun `converts retry toMessage`() {
        assertRoundtrip(SseMessage.Retry(Duration.ofMillis(1000)), "retry: 1000\n\n")
    }

    private fun assertRoundtrip(message: SseMessage, string: String) {
        assertThat(message.toMessage(), equalTo(string))
        assertThat(SseMessage.parse(string), equalTo(message))
    }
}
