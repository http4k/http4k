/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package util.mime

import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.equalTo
import org.apache.commons.fileupload.util.mime.QuotedPrintableDecoder
import org.junit.jupiter.api.Assertions.assertArrayEquals
import org.junit.jupiter.api.Assertions.fail
import org.junit.jupiter.api.Test
import java.io.ByteArrayOutputStream
import java.io.IOException
import java.io.UnsupportedEncodingException

/**
 * @since 1.3
 */
class QuotedPrintableDecoderTestCase {

    @Test
    @Throws(Exception::class)
    fun emptyDecode() {
        assertEncoded("", "")
    }

    @Test
    @Throws(Exception::class)
    fun plainDecode() {
        // spaces are allowed in encoded data
        // There are special rules for trailing spaces; these are not currently implemented.
        assertEncoded("The quick brown fox jumps over the lazy dog.", "The quick brown fox jumps over the lazy dog.")
    }

    @Test
    @Throws(Exception::class)
    fun basicEncodeDecode() {
        assertEncoded("= Hello there =\r\n", "=3D Hello there =3D=0D=0A")
    }

    @Test
    @Throws(Exception::class)
    fun invalidQuotedPrintableEncoding() {
        assertIOException("truncated escape sequence", "YWJjMTIzXy0uKn4hQCMkJV4mKCkre31cIlxcOzpgLC9bXQ==")
    }

    @Test
    @Throws(Exception::class)
    fun unsafeDecode() {
        assertEncoded("=\r\n", "=3D=0D=0A")
    }

    @Test
    @Throws(Exception::class)
    fun unsafeDecodeLowerCase() {
        assertEncoded("=\r\n", "=3d=0d=0a")
    }

    @Test
    @Throws(Exception::class)
    fun invalidCharDecode() {
        try {
            assertEncoded("=\r\n", "=3D=XD=XA")
            fail<Any>("did not throw")
        } catch (e: IOException) {
        }
    }

    /**
     * This is NOT supported by Commons-Codec, see CODEC-121.
     *
     * @throws Exception
     * @see [CODEC-121](https://issues.apache.org/jira/browse/CODEC-121)
     */
    @Test
    @Throws(Exception::class)
    fun softLineBreakDecode() {
        assertEncoded("If you believe that truth=beauty, then surely mathematics is the most beautiful branch of philosophy.",
            "If you believe that truth=3Dbeauty, then surely=20=\r\nmathematics is the most beautiful branch of philosophy.")
    }

    @Test
    @Throws(Exception::class)
    fun invalidSoftBreak1() {
        assertIOException("CR must be followed by LF", "=\r\r")
    }

    @Test
    @Throws(Exception::class)
    fun invalidSoftBreak2() {
        assertIOException("CR must be followed by LF", "=\rn")
    }

    @Test
    @Throws(Exception::class)
    fun truncatedEscape() {
        assertIOException("truncated", "=1")
    }

    companion object {

        private val US_ASCII_CHARSET = "US-ASCII"

        @Throws(Exception::class)
        private fun assertEncoded(clearText: String, encoded: String) {
            val expected = clearText.toByteArray(charset(US_ASCII_CHARSET))

            val out = ByteArrayOutputStream(encoded.length)
            val encodedData = encoded.toByteArray(charset(US_ASCII_CHARSET))
            QuotedPrintableDecoder.decode(encodedData, out)
            val actual = out.toByteArray()

            assertArrayEquals(expected, actual)
        }

        @Throws(UnsupportedEncodingException::class)
        private fun assertIOException(messageText: String, encoded: String) {
            val out = ByteArrayOutputStream(encoded.length)
            val encodedData = encoded.toByteArray(charset(US_ASCII_CHARSET))
            try {
                QuotedPrintableDecoder.decode(encodedData, out)
                fail<Any>("Expected IOException")
            } catch (e: IOException) {
                val em = e.message!!
                assertThat("Expected to find $messageText in '$em'", em.contains(messageText), equalTo(true))
            }
        }
    }
}
