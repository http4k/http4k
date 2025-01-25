package org.http4k.mcp.internal

import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.equalTo
import org.junit.jupiter.api.Test
import java.io.StringWriter

class DebuggingWriterTest {

    @Test
    fun `writes output`() {
        val output = StringWriter()
        val written = StringWriter()
        val input = "hello\nworld\n"
        DebuggingWriter(output, written).write(input)
        assertThat(written.buffer.take(input.length), equalTo(input))
    }
}
