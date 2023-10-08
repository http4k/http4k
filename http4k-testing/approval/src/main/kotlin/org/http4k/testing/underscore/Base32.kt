/*
 * The MIT License (MIT)
 *
 * Copyright 2015-2023 Valentyn Kolesnikov
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */
package org.http4k.testing.underscore

import java.nio.charset.StandardCharsets

class Base32 private constructor() {
    private val digits: CharArray
    private val mask: Int
    private val shift: Int
    private val charMap: MutableMap<Char, Int>

    init {
        digits = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdef".toCharArray()
        mask = digits.size - 1
        shift = Integer.numberOfTrailingZeros(digits.size)
        charMap = HashMap()
        var index = 0
        while (index < digits.size) {
            charMap[digits[index]] = index
            index += 1
        }
    }

    private fun decodeInternal(encoded: String): ByteArray {
        if (encoded.length == 0) {
            return ByteArray(0)
        }
        val encodedLength = encoded.length
        val outLength = encodedLength * shift / 8
        val result = ByteArray(outLength)
        var buffer = 0
        var next = 0
        var bitsLeft = 0
        for (c in encoded.toCharArray()) {
            if (!charMap.containsKey(c)) {
                throw DecodingException("Illegal character: $c")
            }
            buffer = buffer shl shift
            buffer = buffer or (charMap[c]!! and mask)
            bitsLeft += shift
            if (bitsLeft >= 8) {
                result[next++] = (buffer shr bitsLeft - 8).toByte()
                bitsLeft -= 8
            }
        }
        return result
    }

    private fun encodeInternal(data: ByteArray): String {
        if (data.size == 0) {
            return ""
        }
        val outputLength = (data.size * 8 + shift - 1) / shift
        val result = StringBuilder(outputLength)
        var buffer = data[0].toInt()
        var next = 1
        var bitsLeft = 8
        while (bitsLeft > 0 || next < data.size) {
            if (bitsLeft < shift) {
                if (next < data.size) {
                    buffer = buffer shl 8
                    buffer = buffer or (data[next++].toInt() and 0xff)
                    bitsLeft += 8
                } else {
                    val pad = shift - bitsLeft
                    buffer = buffer shl pad
                    bitsLeft += pad
                }
            }
            val index = mask and (buffer shr bitsLeft - shift)
            bitsLeft -= shift
            result.append(digits[index])
        }
        return result.toString()
    }

    class DecodingException(message: String?) : RuntimeException(message)
    companion object {
        private val INSTANCE = Base32()

        @JvmStatic
        fun decode(encoded: String): String {
            return String(INSTANCE.decodeInternal(encoded), StandardCharsets.UTF_8)
        }

        @JvmStatic
        fun encode(data: String): String {
            return INSTANCE.encodeInternal(data.toByteArray(StandardCharsets.UTF_8))
        }
    }
}
