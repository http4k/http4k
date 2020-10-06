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

import org.apache.commons.fileupload.util.mime.Base64Decoder
import org.junit.jupiter.api.Assertions.assertArrayEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Assertions.fail
import org.junit.jupiter.api.Test
import java.io.ByteArrayOutputStream
import java.io.IOException
import java.io.UnsupportedEncodingException

/**
 * @since 1.3
 */
class Base64DecoderTestCase {

    /**
     * Tests RFC 4648 section 10 test vectors.
     *
     *  * BASE64("") = ""
     *  * BASE64("f") = "Zg=="
     *  * BASE64("fo") = "Zm8="
     *  * BASE64("foo") = "Zm9v"
     *  * BASE64("foob") = "Zm9vYg=="
     *  * BASE64("fooba") = "Zm9vYmE="
     *  * BASE64("foobar") = "Zm9vYmFy"
     *
     *
     * @see [http://tools.ietf.org/html/rfc4648](http://tools.ietf.org/html/rfc4648)
     */
    @Test
    @Throws(Exception::class)
    fun rfc4648Section10Decode() {
        assertEncoded("", "")
        assertEncoded("f", "Zg==")
        assertEncoded("fo", "Zm8=")
        assertEncoded("foo", "Zm9v")
        assertEncoded("foob", "Zm9vYg==")
        assertEncoded("fooba", "Zm9vYmE=")
        assertEncoded("foobar", "Zm9vYmFy")
    }

    /**
     * Test our decode with pad character in the middle.
     * Continues provided that the padding is in the correct place,
     * i.e. concatenated valid strings decode OK.
     */
    @Test
    @Throws(Exception::class)
    fun decodeWithInnerPad() {
        assertEncoded("Hello WorldHello World", "SGVsbG8gV29ybGQ=SGVsbG8gV29ybGQ=")
    }

    /**
     * Ignores non-BASE64 bytes.
     */
    @Test
    @Throws(Exception::class)
    fun nonBase64Bytes() {
        assertEncoded("Hello World", "S?G!V%sbG 8g\rV\t\n29ybGQ*=")
    }

    @Test
    @Throws(Exception::class)
    fun truncatedString() {
        try {
            val x = byteArrayOf('n'.toByte())
            Base64Decoder.decode(x, ByteArrayOutputStream())
        } catch (e: IOException) {
        }
    }

    @Test
    @Throws(Exception::class)
    fun decodeTrailingJunk() {
        assertEncoded("foobar", "Zm9vYmFy!!!")
    }

    // If there are valid trailing Base64 chars, complain
    @Test
    @Throws(Exception::class)
    fun decodeTrailing1() {
        assertIOException("truncated", "Zm9vYmFy1")
    }

    // If there are valid trailing Base64 chars, complain
    @Test
    @Throws(Exception::class)
    fun decodeTrailing2() {
        assertIOException("truncated", "Zm9vYmFy12")
    }

    // If there are valid trailing Base64 chars, complain
    @Test
    @Throws(Exception::class)
    fun decodeTrailing3() {
        assertIOException("truncated", "Zm9vYmFy123")
    }

    @Test
    @Throws(Exception::class)
    fun badPadding() {
        assertIOException("incorrect padding, 4th byte", "Zg=a")
    }

    @Test
    @Throws(Exception::class)
    fun badPaddingLeading1() {
        assertIOException("incorrect padding, first two bytes cannot be padding", "=A==")
    }

    @Test
    @Throws(Exception::class)
    fun badPaddingLeading2() {
        assertIOException("incorrect padding, first two bytes cannot be padding", "====")
    }

    // This input causes java.lang.ArrayIndexOutOfBoundsException: 1
    // in the Java 6 method DatatypeConverter.parseBase64Binary(String)
    // currently reported as truncated (the last chunk consists just of '=')
    @Test
    @Throws(Exception::class)
    fun badLength() {
        assertIOException("truncated", "Zm8==")
    }

    // These inputs cause java.lang.ArrayIndexOutOfBoundsException
    // in the Java 6 method DatatypeConverter.parseBase64Binary(String)
    // The non-ASCII characters should just be ignored
    @Test
    @Throws(Exception::class)
    fun nonASCIIcharacter() {
        assertEncoded("f", "Zg=À=") // A-grave
        assertEncoded("f", "Zg=\u0100=")
    }

    companion object {

        private val US_ASCII_CHARSET = "US-ASCII"

        @Throws(Exception::class)
        private fun assertEncoded(clearText: String, encoded: String) {
            val expected = clearText.toByteArray(charset(US_ASCII_CHARSET))

            val out = ByteArrayOutputStream(encoded.length)
            val encodedData = encoded.toByteArray(charset(US_ASCII_CHARSET))
            Base64Decoder.decode(encodedData, out)
            val actual = out.toByteArray()

            assertArrayEquals(expected, actual)
        }

        @Throws(UnsupportedEncodingException::class)
        private fun assertIOException(messageText: String, encoded: String) {
            val out = ByteArrayOutputStream(encoded.length)
            val encodedData = encoded.toByteArray(charset(US_ASCII_CHARSET))
            try {
                Base64Decoder.decode(encodedData, out)
                fail<Any>("Expected IOException")
            } catch (e: IOException) {
                val em = e.message!!
                assertTrue(em.contains(messageText))
            }
        }
    }
}
