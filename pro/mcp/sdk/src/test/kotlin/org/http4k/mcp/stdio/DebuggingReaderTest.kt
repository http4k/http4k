package org.http4k.mcp.stdio

import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.equalTo
import org.junit.jupiter.api.Test
import java.io.StringWriter

class DebuggingReaderTest {

    @Test
    fun `writes input`() {
        val input = listOf("hello\n", "world\n").joinToString("")
        val writer = StringWriter()
        DebuggingReader(input.reader(), writer).read(CharArray(100000))
        assertThat(writer.buffer.toString().take(input.length), equalTo(input))
    }
}
