package org.http4k.multipart

import java.io.ByteArrayOutputStream
import java.io.IOException
import java.nio.charset.Charset
import java.nio.charset.StandardCharsets
import java.util.*
import java.util.Arrays.asList

class ValidMultipartFormBuilder(boundary: ByteArray, private val encoding: Charset) {
    private val boundary = ArrayDeque<ByteArray>()
    private val builder = ByteArrayOutputStream()

    constructor(boundary: String) : this(boundary.toByteArray(StandardCharsets.UTF_8), StandardCharsets.UTF_8) {}

    init {
        this.boundary.push(StreamingMultipartFormParts.prependBoundaryWithStreamTerminator(boundary))
    }

    fun build(): ByteArray {
        try {
            builder.write(boundary.peek())
            builder.write(StreamingMultipartFormParts.STREAM_TERMINATOR)
            builder.write(StreamingMultipartFormParts.FIELD_SEPARATOR)
            return builder.toByteArray()
        } catch (e: Exception) {
            throw RuntimeException(e)
        }

    }

    fun field(name: String, value: String): ValidMultipartFormBuilder {
        part(value, Pair("Content-Disposition", listOf(Pair("form-data", null), Pair("name", name))))
        return this
    }

    private fun appendHeader(headerName: String, pairs: List<Pair<String, String?>>) = try {
        val headers = headerName + ": " + pairs.joinToString("; ") { (first, second) ->
            if (second != null) """$first="$second"""" else first
        }

        builder.write(headers.toByteArray(encoding))
        builder.write(StreamingMultipartFormParts.FIELD_SEPARATOR)
    } catch (e: IOException) {
        throw RuntimeException(e)
    }

    fun part(contents: String, vararg headers: Pair<String, List<Pair<String, String?>>>): ValidMultipartFormBuilder {
        try {
            builder.write(boundary.peek())
            builder.write(StreamingMultipartFormParts.FIELD_SEPARATOR)
            asList(*headers).forEach { (first, second) -> appendHeader(first, second) }
            builder.write(StreamingMultipartFormParts.FIELD_SEPARATOR)
            builder.write(contents.toByteArray(encoding))
            builder.write(StreamingMultipartFormParts.FIELD_SEPARATOR)
            return this
        } catch (e: IOException) {
            throw RuntimeException(e)
        }

    }

    fun file(fieldName: String, filename: String, contentType: String, contents: String): ValidMultipartFormBuilder {
        part(contents,
            Pair("Content-Disposition", listOf(Pair("form-data", null), Pair("name", fieldName), Pair("filename", filename))),
            Pair("Content-Type", listOf(Pair(contentType, null)))
        )
        return this
    }

    fun rawPart(raw: String): ValidMultipartFormBuilder {
        try {
            builder.write(boundary.peek())
            builder.write(StreamingMultipartFormParts.FIELD_SEPARATOR)
            builder.write(raw.toByteArray(encoding))
            builder.write(StreamingMultipartFormParts.FIELD_SEPARATOR)
            return this
        } catch (e: IOException) {
            throw RuntimeException(e)
        }

    }

    fun startMultipart(multipartFieldName: String, subpartBoundary: String): ValidMultipartFormBuilder {
        try {
            builder.write(boundary.peek())
            builder.write(StreamingMultipartFormParts.FIELD_SEPARATOR)
            appendHeader("Content-Disposition", listOf(Pair("form-data", null), Pair("name", multipartFieldName)))
            appendHeader("Content-Type", listOf(Pair("multipart/mixed", null), Pair("boundary", subpartBoundary)))
            builder.write(StreamingMultipartFormParts.FIELD_SEPARATOR)
            boundary.push((String(StreamingMultipartFormParts.STREAM_TERMINATOR, encoding) + subpartBoundary).toByteArray(encoding))
            return this
        } catch (e: IOException) {
            throw RuntimeException(e)
        }

    }

    fun attachment(fileName: String, contentType: String, contents: String): ValidMultipartFormBuilder {
        part(contents,
            Pair("Content-Disposition", listOf(Pair("attachment", null), Pair("filename", fileName))),
            Pair("Content-Type", listOf(Pair(contentType, null)))
        )
        return this
    }

    fun endMultipart(): ValidMultipartFormBuilder {
        try {
            builder.write(boundary.pop())
            builder.write(StreamingMultipartFormParts.STREAM_TERMINATOR)
            builder.write(StreamingMultipartFormParts.FIELD_SEPARATOR)
            return this
        } catch (e: IOException) {
            throw RuntimeException(e)
        }

    }
}
