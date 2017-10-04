package org.http4k.multipart

import java.io.ByteArrayOutputStream
import java.nio.charset.Charset
import java.nio.charset.StandardCharsets
import java.util.*

class ValidMultipartFormBuilder(boundary: ByteArray, private val encoding: Charset = Charset.defaultCharset()) {
    private val boundary = ArrayDeque<ByteArray>()
    private val builder = ByteArrayOutputStream()

    constructor(boundary: String) : this(boundary.toByteArray(StandardCharsets.UTF_8), StandardCharsets.UTF_8) {}

    init {
        this.boundary.push(StreamingMultipartFormParts.prependBoundaryWithStreamTerminator(boundary))
    }

    fun build(): ByteArray {
        builder.write(boundary.peek())
        builder.write(StreamingMultipartFormParts.STREAM_TERMINATOR)
        builder.write(StreamingMultipartFormParts.FIELD_SEPARATOR)
        return builder.toByteArray()
    }

    fun field(name: String, value: String): ValidMultipartFormBuilder {
        part(value, Pair("Content-Disposition", listOf("form-data" to null, "name" to name)))
        return this
    }

    private fun appendHeader(headerName: String, pairs: List<Pair<String, String?>>) {
        val headers = headerName + ": " + pairs.joinToString("; ") { (first, second) ->
            if (second != null) """$first="$second"""" else first
        }

        builder.write(headers.toByteArray(encoding))
        builder.write(StreamingMultipartFormParts.FIELD_SEPARATOR)
    }

    fun file(fieldName: String, filename: String, contentType: String, contents: String): ValidMultipartFormBuilder {
        part(contents,
            "Content-Disposition" to listOf("form-data" to null, "name" to fieldName, "filename" to filename),
            "Content-Type" to listOf(contentType to null)
        )
        return this
    }

    fun part(contents: String, vararg headers: Pair<String, List<Pair<String, String?>>>): ValidMultipartFormBuilder {
        builder.write(boundary.peek())
        builder.write(StreamingMultipartFormParts.FIELD_SEPARATOR)
        if(headers.isNotEmpty()) {
            headers.toList().forEach { (first, second) -> appendHeader(first, second) }
            builder.write(StreamingMultipartFormParts.FIELD_SEPARATOR)
        }
        builder.write(contents.toByteArray(encoding))
        builder.write(StreamingMultipartFormParts.FIELD_SEPARATOR)
        return this
    }

    fun startMultipart(multipartFieldName: String, subpartBoundary: String): ValidMultipartFormBuilder {
        builder.write(boundary.peek())
        builder.write(StreamingMultipartFormParts.FIELD_SEPARATOR)
        appendHeader("Content-Disposition", listOf(Pair("form-data", null), Pair("name", multipartFieldName)))
        appendHeader("Content-Type", listOf(Pair("multipart/mixed", null), Pair("boundary", subpartBoundary)))
        builder.write(StreamingMultipartFormParts.FIELD_SEPARATOR)
        boundary.push((String(StreamingMultipartFormParts.STREAM_TERMINATOR, encoding) + subpartBoundary).toByteArray(encoding))
        return this
    }

    fun attachment(fileName: String, contentType: String, contents: String): ValidMultipartFormBuilder {
        part(contents,
            Pair("Content-Disposition", listOf(Pair("attachment", null), Pair("filename", fileName))),
            Pair("Content-Type", listOf(Pair(contentType, null)))
        )
        return this
    }

    fun endMultipart(): ValidMultipartFormBuilder {
        builder.write(boundary.pop())
        builder.write(StreamingMultipartFormParts.STREAM_TERMINATOR)
        builder.write(StreamingMultipartFormParts.FIELD_SEPARATOR)
        return this
    }
}
