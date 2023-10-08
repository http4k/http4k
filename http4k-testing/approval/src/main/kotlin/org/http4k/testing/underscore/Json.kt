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

import java.math.BigDecimal
import java.math.BigInteger
import java.util.*

object Json {
    private const val NULL = "null"
    private const val DIGIT = "digit"

    @JvmOverloads
    @JvmStatic
    fun toJson(
        collection: Collection<*>?,
        identStep: JsonStringBuilder.Step = JsonStringBuilder.Step.TWO_SPACES
    ): String {
        val builder = JsonStringBuilder(identStep)
        JsonArray.writeJson(collection, builder)
        return builder.toString()
    }

    @JvmOverloads
    @JvmStatic
    fun toJson(map: Map<*, *>?, identStep: JsonStringBuilder.Step = JsonStringBuilder.Step.TWO_SPACES): String {
        val builder = JsonStringBuilder(identStep)
        JsonObject.writeJson(map, builder)
        return builder.toString()
    }

    @JvmStatic
    fun fromJson(string: String): Any? {
        return JsonParser(string).parse()
    }

    @JvmStatic
    @JvmOverloads
    fun formatJson(json: String, identStep: JsonStringBuilder.Step = JsonStringBuilder.Step.TWO_SPACES): String {
        val result = fromJson(json)
        return if (result is Map<*, *>) {
            toJson(result as Map<*, *>?, identStep)
        } else toJson(result as List<*>?, identStep)
    }

    class JsonStringBuilder {
        enum class Step(val indent: Int) {
            TWO_SPACES(2),
            THREE_SPACES(3),
            FOUR_SPACES(4),
            COMPACT(0),
            TABS(1)

        }

        private val builder: StringBuilder
        val identStep: Step
        private var indent = 0

        constructor(identStep: Step) {
            builder = StringBuilder()
            this.identStep = identStep
        }

        constructor() {
            builder = StringBuilder()
            identStep = Step.TWO_SPACES
        }

        fun append(character: Char): JsonStringBuilder {
            builder.append(character)
            return this
        }

        fun append(string: String?): JsonStringBuilder {
            builder.append(string)
            return this
        }

        fun fillSpaces(): JsonStringBuilder {
            var index = 0
            while (index < indent) {
                builder.append(if (identStep == Step.TABS) '\t' else ' ')
                index += 1
            }
            return this
        }

        fun incIndent(): JsonStringBuilder {
            indent += identStep.indent
            return this
        }

        fun decIndent(): JsonStringBuilder {
            indent -= identStep.indent
            return this
        }

        fun newLine(): JsonStringBuilder {
            if (identStep != Step.COMPACT) {
                builder.append('\n')
            }
            return this
        }

        override fun toString(): String {
            return builder.toString()
        }
    }

    object JsonArray {
        @JvmStatic
        fun writeJson(collection: Collection<*>?, builder: JsonStringBuilder) {
            if (collection == null) {
                builder.append(NULL)
                return
            }
            val iter = collection.iterator()
            builder.append('[').incIndent()
            if (!collection.isEmpty()) {
                builder.newLine()
            }
            while (iter.hasNext()) {
                val value = iter.next()
                builder.fillSpaces()
                JsonValue.writeJson(value, builder)
                if (iter.hasNext()) {
                    builder.append(',').newLine()
                }
            }
            builder.newLine().decIndent().fillSpaces().append(']')
        }

        @JvmStatic
        fun writeJson(array: ByteArray?, builder: JsonStringBuilder) {
            if (array == null) {
                builder.append(NULL)
            } else if (array.isEmpty()) {
                builder.append("[]")
            } else {
                builder.append('[').incIndent().newLine()
                builder.fillSpaces().append(array[0].toString())
                for (i in 1 until array.size) {
                    builder.append(',').newLine().fillSpaces()
                    builder.append(array[i].toString())
                }
                builder.newLine().decIndent().fillSpaces().append(']')
            }
        }

        @JvmStatic
        fun writeJson(array: ShortArray?, builder: JsonStringBuilder) {
            if (array == null) {
                builder.append(NULL)
            } else if (array.isEmpty()) {
                builder.append("[]")
            } else {
                builder.append('[').incIndent().newLine()
                builder.fillSpaces().append(array[0].toString())
                for (i in 1 until array.size) {
                    builder.append(',').newLine().fillSpaces()
                    builder.append(array[i].toString())
                }
                builder.newLine().decIndent().fillSpaces().append(']')
            }
        }

        @JvmStatic
        fun writeJson(array: IntArray?, builder: JsonStringBuilder) {
            if (array == null) {
                builder.append(NULL)
            } else if (array.isEmpty()) {
                builder.append("[]")
            } else {
                builder.append('[').incIndent().newLine()
                builder.fillSpaces().append(array[0].toString())
                for (i in 1 until array.size) {
                    builder.append(',').newLine().fillSpaces()
                    builder.append(array[i].toString())
                }
                builder.newLine().decIndent().fillSpaces().append(']')
            }
        }

        @JvmStatic
        fun writeJson(array: LongArray?, builder: JsonStringBuilder) {
            if (array == null) {
                builder.append(NULL)
            } else if (array.isEmpty()) {
                builder.append("[]")
            } else {
                builder.append('[').incIndent().newLine()
                builder.fillSpaces().append(array[0].toString())
                for (i in 1 until array.size) {
                    builder.append(',').newLine().fillSpaces()
                    builder.append(array[i].toString())
                }
                builder.newLine().decIndent().fillSpaces().append(']')
            }
        }

        @JvmStatic
        fun writeJson(array: FloatArray?, builder: JsonStringBuilder) {
            if (array == null) {
                builder.append(NULL)
            } else if (array.isEmpty()) {
                builder.append("[]")
            } else {
                builder.append('[').incIndent().newLine()
                builder.fillSpaces().append(array[0].toString())
                for (i in 1 until array.size) {
                    builder.append(',').newLine().fillSpaces()
                    builder.append(array[i].toString())
                }
                builder.newLine().decIndent().fillSpaces().append(']')
            }
        }

        @JvmStatic
        fun writeJson(array: DoubleArray?, builder: JsonStringBuilder) {
            if (array == null) {
                builder.append(NULL)
            } else if (array.isEmpty()) {
                builder.append("[]")
            } else {
                builder.append('[').incIndent().newLine()
                builder.fillSpaces().append(array[0].toString())
                for (i in 1 until array.size) {
                    builder.append(',').newLine().fillSpaces()
                    builder.append(array[i].toString())
                }
                builder.newLine().decIndent().fillSpaces().append(']')
            }
        }

        @JvmStatic
        fun writeJson(array: BooleanArray?, builder: JsonStringBuilder) {
            if (array == null) {
                builder.append(NULL)
            } else if (array.isEmpty()) {
                builder.append("[]")
            } else {
                builder.append('[').incIndent().newLine()
                builder.fillSpaces().append(array[0].toString())
                for (i in 1 until array.size) {
                    builder.append(',').newLine().fillSpaces()
                    builder.append(array[i].toString())
                }
                builder.newLine().decIndent().fillSpaces().append(']')
            }
        }

        @JvmStatic
        fun writeJson(array: CharArray?, builder: JsonStringBuilder) {
            if (array == null) {
                builder.append(NULL)
            } else if (array.isEmpty()) {
                builder.append("[]")
            } else {
                builder.append('[').incIndent().newLine()
                builder.fillSpaces().append('\"').append(array[0].toString()).append('\"')
                for (i in 1 until array.size) {
                    builder.append(',').newLine().fillSpaces()
                    builder.append('"').append(array[i].toString()).append('"')
                }
                builder.newLine().decIndent().fillSpaces().append(']')
            }
        }

        @JvmStatic
        fun writeJson(array: Array<Any?>?, builder: JsonStringBuilder) {
            if (array == null) {
                builder.append(NULL)
            } else if (array.isEmpty()) {
                builder.append("[]")
            } else {
                builder.append('[').newLine().incIndent().fillSpaces()
                JsonValue.writeJson(array[0], builder)
                for (i in 1 until array.size) {
                    builder.append(',').newLine().fillSpaces()
                    JsonValue.writeJson(array[i], builder)
                }
                builder.newLine().decIndent().fillSpaces().append(']')
            }
        }
    }

    object JsonObject {
        fun writeJson(map: Map<*, *>?, builder: JsonStringBuilder) {
            if (map == null) {
                builder.append(NULL)
                return
            }
            val iter: Iterator<*> = map.entries.iterator()
            builder.append('{').incIndent()
            if (map.isNotEmpty()) {
                builder.newLine()
            }
            while (iter.hasNext()) {
                val (key, value) = iter.next() as Map.Entry<*, *>
                builder.fillSpaces().append('"')
                builder.append(JsonValue.escape(key.toString()))
                builder.append('"')
                builder.append(':')
                if (builder.identStep != JsonStringBuilder.Step.COMPACT) {
                    builder.append(' ')
                }
                JsonValue.writeJson(value, builder)
                if (iter.hasNext()) {
                    builder.append(',').newLine()
                }
            }
            builder.newLine().decIndent().fillSpaces().append('}')
        }
    }

    object JsonValue {
        fun writeJson(value: Any?, builder: JsonStringBuilder) {
            if (value == null) {
                builder.append(NULL)
            } else if (value is String) {
                builder.append('"').append(escape(value as String?)).append('"')
            } else if (value is Double) {
                if (value.isInfinite() || value.isNaN()) {
                    builder.append(NULL)
                } else {
                    builder.append(value.toString())
                }
            } else if (value is Float) {
                if (value.isInfinite() || value.isNaN()) {
                    builder.append(NULL)
                } else {
                    builder.append(value.toString())
                }
            } else if (value is Number) {
                builder.append(value.toString())
            } else if (value is Boolean) {
                builder.append(value.toString())
            } else if (value is Map<*, *>) {
                JsonObject.writeJson(value as Map<*, *>?, builder)
            } else if (value is Collection<*>) {
                JsonArray.writeJson(value as Collection<*>?, builder)
            } else {
                doWriteJson(value, builder)
            }
        }

        @Suppress("UNCHECKED_CAST")
        private fun doWriteJson(value: Any, builder: JsonStringBuilder) {
            if (value is ByteArray) {
                JsonArray.writeJson(value, builder)
            } else if (value is ShortArray) {
                JsonArray.writeJson(value, builder)
            } else if (value is IntArray) {
                JsonArray.writeJson(value, builder)
            } else if (value is LongArray) {
                JsonArray.writeJson(value, builder)
            } else if (value is FloatArray) {
                JsonArray.writeJson(value, builder)
            } else if (value is DoubleArray) {
                JsonArray.writeJson(value, builder)
            } else if (value is BooleanArray) {
                JsonArray.writeJson(value, builder)
            } else if (value is CharArray) {
                JsonArray.writeJson(value, builder)
            } else if (value is Array<*> && value.isArrayOf<Any>()) {
                JsonArray.writeJson(value as Array<Any?>, builder)
            } else {
                builder.append('"').append(escape(value.toString())).append('"')
            }
        }

        @JvmStatic
        fun escape(s: String?): String? {
            if (s == null) {
                return null
            }
            val sb = StringBuilder()
            escape(s, sb)
            return sb.toString()
        }

        private fun escape(s: String, sb: StringBuilder) {
            val len = s.length
            for (i in 0 until len) {
                val ch = s[i]
                when (ch) {
                    '"' -> sb.append("\\\"")
                    '\\' -> sb.append("\\\\")
                    '\b' -> sb.append("\\b")
                    '\u000c' -> sb.append("\\f")
                    '\n' -> sb.append("\\n")
                    '\r' -> sb.append("\\r")
                    '\t' -> sb.append("\\t")
                    '€' -> sb.append('€')
                    else -> if (ch <= '\u001F' || ch >= '\u007F' && ch <= '\u009F' || ch >= '\u2000' && ch <= '\u20FF') {
                        val ss = Integer.toHexString(ch.code)
                        sb.append("\\u")
                        var k = 0
                        while (k < 4 - ss.length) {
                            sb.append("0")
                            k++
                        }
                        sb.append(ss.uppercase(Locale.getDefault()))
                    } else {
                        sb.append(ch)
                    }
                }
            }
        }
    }

    class ParseException(
        message: String?,
        @JvmField val offset: Int,
        @JvmField val line: Int,
        @JvmField val column: Int
    ) :
        RuntimeException(String.format("%s at %d:%d", message, line, column))

    class JsonParser(private val json: String) {
        private var index = 0
        private var line = 1
        private var lineOffset = 0
        private var current = 0
        private var captureBuffer: StringBuilder? = null
        private var captureStart: Int

        init {
            captureStart = -1
        }

        fun parse(): Any? {
            read()
            skipWhiteSpace()
            val result = readValue()
            skipWhiteSpace()
            if (!isEndOfText) {
                throw error("Unexpected character")
            }
            return result
        }

        private fun readValue(): Any? {
            return when (current.toChar()) {
                'n' -> readNull()
                't' -> readTrue()
                'f' -> readFalse()
                '"' -> readString()
                '[' -> readArray()
                '{' -> readObject()
                '-', '0', '1', '2', '3', '4', '5', '6', '7', '8', '9' -> readNumber()
                else -> throw expected("value")
            }
        }

        private fun readArray(): List<Any?> {
            read()
            val array: MutableList<Any?> = ArrayList()
            skipWhiteSpace()
            if (readChar(']')) {
                return array
            }
            do {
                skipWhiteSpace()
                array.add(readValue())
                skipWhiteSpace()
            } while (readChar(','))
            if (!readChar(']')) {
                throw expected("',' or ']'")
            }
            return array
        }

        private fun readObject(): Map<String, Any?> {
            read()
            val `object`: MutableMap<String, Any?> = LinkedHashMap()
            skipWhiteSpace()
            if (readChar('}')) {
                return `object`
            }
            do {
                skipWhiteSpace()
                val name = readName()
                skipWhiteSpace()
                if (!readChar(':')) {
                    throw expected("':'")
                }
                skipWhiteSpace()
                `object`[name] = readValue()
                skipWhiteSpace()
            } while (readChar(','))
            if (!readChar('}')) {
                throw expected("',' or '}'")
            }
            return `object`
        }

        private fun readName(): String {
            if (current != '"'.code) {
                throw expected("name")
            }
            return readString()
        }

        private fun readNull(): String? {
            read()
            readRequiredChar('u')
            readRequiredChar('l')
            readRequiredChar('l')
            return null
        }

        private fun readTrue(): Boolean {
            read()
            readRequiredChar('r')
            readRequiredChar('u')
            readRequiredChar('e')
            return true
        }

        private fun readFalse(): Boolean {
            read()
            readRequiredChar('a')
            readRequiredChar('l')
            readRequiredChar('s')
            readRequiredChar('e')
            return false
        }

        private fun readRequiredChar(ch: Char) {
            if (!readChar(ch)) {
                throw expected("'$ch'")
            }
        }

        private fun readString(): String {
            read()
            startCapture()
            while (current != '"'.code) {
                if (current == '\\'.code) {
                    pauseCapture()
                    readEscape()
                    startCapture()
                } else if (current < 0x20) {
                    throw expected("valid string character")
                } else {
                    read()
                }
            }
            val string = endCapture()
            read()
            return string
        }

        private fun readEscape() {
            read()
            when (current.toChar()) {
                '"', '/', '\\' -> captureBuffer!!.append(current.toChar())
                'b' -> captureBuffer!!.append('\b')
                'f' -> captureBuffer!!.append('\u000c')
                'n' -> captureBuffer!!.append('\n')
                'r' -> captureBuffer!!.append('\r')
                't' -> captureBuffer!!.append('\t')
                'u' -> {
                    val hexChars = CharArray(4)
                    var isHexCharsDigits = true
                    var i = 0
                    while (i < 4) {
                        read()
                        if (!isHexDigit) {
                            isHexCharsDigits = false
                        }
                        hexChars[i] = current.toChar()
                        i++
                    }
                    if (isHexCharsDigits) {
                        captureBuffer!!.append(String(hexChars).toInt(16).toChar())
                    } else {
                        captureBuffer
                            ?.append("\\u")
                            ?.append(hexChars[0])
                            ?.append(hexChars[1])
                            ?.append(hexChars[2])
                            ?.append(hexChars[3])
                    }
                }

                else -> throw expected("valid escape sequence")
            }
            read()
        }

        private fun readNumber(): Number {
            startCapture()
            readChar('-')
            val firstDigit = current
            if (!readDigit()) {
                throw expected(DIGIT)
            }
            if (firstDigit != '0'.code) {
                while (readDigit()) {
                    // ignored
                }
            }
            readFraction()
            readExponent()
            val number = endCapture()
            val result: Number = if (number.contains(".") || number.contains("e") || number.contains("E")) {
                if (number.length > 9
                    || number.contains(".") && number.length - number.lastIndexOf('.') > 2
                    && number[number.length - 1] == '0'
                ) {
                    BigDecimal(number)
                } else {
                    number.toDouble()
                }
            } else {
                if (number.length > 19) {
                    BigInteger(number)
                } else {
                    number.toLong()
                }
            }
            return result
        }

        private fun readFraction(): Boolean {
            if (!readChar('.')) {
                return false
            }
            if (!readDigit()) {
                throw expected(DIGIT)
            }
            while (readDigit()) {
                // ignored
            }
            return true
        }

        private fun readExponent(): Boolean {
            if (!readChar('e') && !readChar('E')) {
                return false
            }
            if (!readChar('+')) {
                readChar('-')
            }
            if (!readDigit()) {
                throw expected(DIGIT)
            }
            while (readDigit()) {
                // ignored
            }
            return true
        }

        private fun readChar(ch: Char): Boolean {
            if (current != ch.code) {
                return false
            }
            read()
            return true
        }

        private fun readDigit(): Boolean {
            if (!isDigit) {
                return false
            }
            read()
            return true
        }

        private fun skipWhiteSpace() {
            while (isWhiteSpace) {
                read()
            }
        }

        private fun read() {
            if (index == json.length) {
                current = -1
                return
            }
            if (current == '\n'.code) {
                line++
                lineOffset = index
            }
            current = json[index++].code
        }

        private fun startCapture() {
            if (captureBuffer == null) {
                captureBuffer = StringBuilder()
            }
            captureStart = index - 1
        }

        private fun pauseCapture() {
            captureBuffer!!.append(json, captureStart, index - 1)
            captureStart = -1
        }

        private fun endCapture(): String {
            val end = if (current == -1) index else index - 1
            val captured: String
            if (captureBuffer!!.isNotEmpty()) {
                captureBuffer!!.append(json, captureStart, end)
                captured = captureBuffer.toString()
                captureBuffer!!.setLength(0)
            } else {
                captured = json.substring(captureStart, end)
            }
            captureStart = -1
            return captured
        }

        private fun expected(expected: String): ParseException {
            return if (isEndOfText) {
                error("Unexpected end of input")
            } else error("Expected $expected")
        }

        private fun error(message: String): ParseException {
            val absIndex = index
            val column = absIndex - lineOffset
            val offset = if (isEndOfText) absIndex else absIndex - 1
            return ParseException(message, offset, line, column - 1)
        }

        private val isWhiteSpace: Boolean
            get() = current == ' '.code || current == '\t'.code || current == '\n'.code || current == '\r'.code
        private val isDigit: Boolean
            get() = current >= '0'.code && current <= '9'.code
        private val isHexDigit: Boolean
            get() = isDigit || current >= 'a'.code && current <= 'f'.code || current >= 'A'.code && current <= 'F'.code
        private val isEndOfText: Boolean
            get() = current == -1
    }
}
