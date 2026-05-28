package org.http4k.util

import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.equalTo
import com.natpryce.hamkrest.throws
import org.junit.jupiter.api.Test

class SizeLimitedInputStreamTest {

    @Test
    fun `reads a stream that is within the limit`() {
        assertThat(SizeLimitedInputStream(ByteArray(10).inputStream(), 10).readBytes().size, equalTo(10))
    }

    @Test
    fun `throws once the limit is exceeded`() {
        assertThat(
            { SizeLimitedInputStream(ByteArray(10).inputStream(), 9).readBytes() },
            throws<SizeLimitExceededException>()
        )
    }
}
