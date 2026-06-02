package org.http4k.sse

import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.equalTo
import com.natpryce.hamkrest.isEmpty
import org.junit.jupiter.api.Test

class ChunkedSseSequenceTest {

    @Test
    fun `empty sse stream`() {
        assertThat(
            "".byteInputStream().chunkedSseSequence().toList(),
            isEmpty
        )
    }

    @Test
    fun `oversized message is dropped and parser resyncs to the next message`() {
        val huge = "data: " + "x".repeat(200) + "\n\n"
        val small = "data: ok\n\n"

        val emitted = (huge + small).byteInputStream().chunkedSseSequence(maxMessageSize = 100).toList()

        assertThat(emitted, equalTo(listOf<SseMessage>(SseMessage.Data("ok"))))
    }

    @Test
    fun `unterminated oversized stream completes without OOM and emits nothing`() {
        val emitted = "data: ${"x".repeat(10_000)}".byteInputStream().chunkedSseSequence(maxMessageSize = 100).toList()

        assertThat(emitted, isEmpty)
    }

    @Test
    fun `sse stream handles all valid line ending combinations`() {
        listOf(
            "\n\n",      // LF + LF
            "\r\n\r\n",  // CRLF + CRLF (most common)
            "\r\r",      // CR + CR
            "\r\r\n",    // CR + CRLF
            "\r\n\r",    // CRLF + CR
            "\n\r\n",    // LF + CRLF
            "\r\n\n",    // CRLF + LF
            "\n\r"       // LF + CR
        ).forEach {
            checkLineEndingsAreDetected(it)
        }
    }
}

private fun checkLineEndingsAreDetected(lineEnding: String) {
    val messages = listOf(
        SseMessage.Data("hello"),
        SseMessage.Data("there"),
        SseMessage.Data("world"),
    )

    val content = messages.joinToString("") {
        it.toMessage().replace("\n\n", lineEnding)
    }

    assertThat(content.byteInputStream().chunkedSseSequence().toList(), equalTo(messages))
}
