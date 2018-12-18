package org.http4k.multipart

import java.io.InputStream
import java.io.SequenceInputStream
import java.nio.charset.Charset
import java.nio.charset.StandardCharsets
import java.util.ArrayDeque
import java.util.Collections

internal class MultipartFormBuilder(inBoundary: ByteArray, private val encoding: Charset = Charset.defaultCharset()) {
    private val boundary = ArrayDeque<ByteArray>()

    private val waitingToStream = mutableListOf<InputStream>()

    constructor(boundary: String) : this(boundary.toByteArray(StandardCharsets.UTF_8), StandardCharsets.UTF_8)

    init {
        boundary.push(StreamingMultipartFormParts.prependBoundaryWithStreamTerminator(inBoundary))
    }

    fun stream(): InputStream {
        add(boundary.peek())
        add(StreamingMultipartFormParts.STREAM_TERMINATOR)
        add(StreamingMultipartFormParts.FIELD_SEPARATOR)

        return SequenceInputStream(Collections.enumeration(waitingToStream))
    }

    fun field(name: String, value: String): MultipartFormBuilder = apply {
        part(value, Pair("Content-Disposition", listOf("form-data" to null, "name" to name)))
    }

    private fun appendHeader(headerName: String, pairs: List<Pair<String, String?>>) {
        val headers = "$headerName: " + pairs.joinToString("; ") { (first, second) ->
            if (second != null) """$first="$second"""" else first
        }

        add(headers.toByteArray(encoding))
        add(StreamingMultipartFormParts.FIELD_SEPARATOR)
    }

    fun part(contents: String, vararg headers: Pair<String, List<Pair<String, String?>>>): MultipartFormBuilder = part(contents.byteInputStream(encoding), *headers)

    fun part(contents: InputStream, vararg headers: Pair<String, List<Pair<String, String?>>>): MultipartFormBuilder = apply {
        add(boundary.peek())
        add(StreamingMultipartFormParts.FIELD_SEPARATOR)
        if (headers.isNotEmpty()) {
            headers.toList().forEach { (first, second) -> appendHeader(first, second) }
            add(StreamingMultipartFormParts.FIELD_SEPARATOR)
        }
        waitingToStream.add(contents)
        add(StreamingMultipartFormParts.FIELD_SEPARATOR)
    }

    private fun add(bytes: ByteArray) {
        waitingToStream.add(bytes.inputStream())
    }

    fun startMultipart(multipartFieldName: String, subpartBoundary: String): MultipartFormBuilder = apply {
        add(boundary.peek())
        add(StreamingMultipartFormParts.FIELD_SEPARATOR)
        appendHeader("Content-Disposition", listOf(Pair("form-data", null), Pair("name", multipartFieldName)))
        appendHeader("Content-Type", listOf(Pair("multipart/mixed", null), Pair("boundary", subpartBoundary)))
        add(StreamingMultipartFormParts.FIELD_SEPARATOR)
        boundary.push((String(StreamingMultipartFormParts.STREAM_TERMINATOR, encoding) + subpartBoundary).toByteArray(encoding))
    }

    fun attachment(fileName: String, contentType: String, contents: String): MultipartFormBuilder =
        part(contents,
            Pair("Content-Disposition", listOf(Pair("attachment", null), Pair("filename", fileName))),
            Pair("Content-Type", listOf(Pair(contentType, null)))
        )

    fun file(fieldName: String, filename: String, contentType: String, contents: InputStream): MultipartFormBuilder =
        part(contents,
            "Content-Disposition" to listOf("form-data" to null, "name" to fieldName, "filename" to filename),
            "Content-Type" to listOf(contentType to null)
        )

    fun endMultipart(): MultipartFormBuilder = apply {
        add(boundary.pop())
        add(StreamingMultipartFormParts.STREAM_TERMINATOR)
        add(StreamingMultipartFormParts.FIELD_SEPARATOR)
    }
}
