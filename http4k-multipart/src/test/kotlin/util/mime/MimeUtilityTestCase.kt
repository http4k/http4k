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

import org.apache.commons.fileupload.util.mime.MimeUtility
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import java.io.UnsupportedEncodingException

/**
 * Use the online [MimeHeadersDecoder](http://dogmamix.com/MimeHeadersDecoder/)
 * to validate expected values.
 *
 * @since 1.3
 */
class MimeUtilityTestCase {

    @Test
    @Throws(Exception::class)
    fun noNeedToDecode() {
        assertEncoded("abc", "abc")
    }

    @Test
    @Throws(Exception::class)
    fun decodeUtf8QuotedPrintableEncoded() {
        assertEncoded(" hé! àèôu !!!", "=?UTF-8?Q?_h=C3=A9!_=C3=A0=C3=A8=C3=B4u_!!!?=")
    }

    @Test
    @Throws(Exception::class)
    fun decodeUtf8Base64Encoded() {
        assertEncoded(" hé! àèôu !!!", "=?UTF-8?B?IGjDqSEgw6DDqMO0dSAhISE=?=")
    }

    @Test
    @Throws(Exception::class)
    fun decodeIso88591Base64Encoded() {
        assertEncoded(
            "If you can read this you understand the example.",
            "=?ISO-8859-1?B?SWYgeW91IGNhbiByZWFkIHRoaXMgeW8=?= =?ISO-8859-2?B?dSB1bmRlcnN0YW5kIHRoZSBleGFtcGxlLg==?=\"\r\n"
        )
    }

    @Test
    @Throws(Exception::class)
    fun decodeIso88591Base64EncodedWithWhiteSpace() {
        assertEncoded(
            "If you can read this you understand the example.",
            "=?ISO-8859-1?B?SWYgeW91IGNhbiByZWFkIHRoaXMgeW8=?=\t  \r\n   =?ISO-8859-2?B?dSB1bmRlcnN0YW5kIHRoZSBleGFtcGxlLg==?=\"\r\n"
        )
    }

    @Throws(Exception::class)
    private fun assertEncoded(expected: String, encoded: String) {
        assertEquals(expected, MimeUtility.decodeText(encoded))
    }

    @Test
    @Throws(Exception::class)
    fun decodeInvalidEncoding() {
        try {
            MimeUtility.decodeText("=?invalid?B?xyz-?=")
        } catch (e: UnsupportedEncodingException) {
        }
    }
}
