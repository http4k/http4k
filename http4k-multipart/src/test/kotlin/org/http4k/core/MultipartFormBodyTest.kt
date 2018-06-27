package org.http4k.core

import com.natpryce.hamkrest.Matcher
import com.natpryce.hamkrest.allElements
import com.natpryce.hamkrest.equalTo
import com.natpryce.hamkrest.should.shouldMatch
import org.http4k.lens.Header
import org.junit.jupiter.api.Test
import java.io.InputStream

class MultipartFormBodyTest {

    @Test
    fun `roundtrip`() {
        val form = MultipartFormBody("bob").plus("field" to "bar")
            .plus("file" to FormFile("foo.txt", ContentType.TEXT_PLAIN, "content".byteInputStream()))

        val req = Request(Method.POST, "")
            .with(Header.Common.CONTENT_TYPE of ContentType.MultipartFormWithBoundary(form.boundary))
            .body(form)

        MultipartFormBody.from(req) shouldMatch equalTo(
            MultipartFormBody("bob").plus("field" to "bar")
                .plus("file" to FormFile("foo.txt", ContentType.TEXT_PLAIN, "content".byteInputStream()))
        )
    }

    @Test
    fun `can handle when body is already pulled into memory`() {
        val form = MultipartFormBody("bob").plus("field" to "bar")
            .plus("file" to FormFile("foo.txt", ContentType.TEXT_PLAIN, "content".byteInputStream()))

        val req = Request(Method.POST, "")
            .with(Header.Common.CONTENT_TYPE of ContentType.MultipartFormWithBoundary(form.boundary))
            .body(form)

        req.bodyString()

        MultipartFormBody.from(req) shouldMatch equalTo(
            MultipartFormBody("bob").plus("field" to "bar")
                .plus("file" to FormFile("foo.txt", ContentType.TEXT_PLAIN, "content".byteInputStream()))
        )
    }

    @Test
    fun `closing streams - manually created multipart`() {
        val streams = (1..3).map { "content $it" }.map { TestInputStream(it) }

        val body = streams.toMultipartForm()

        streams shouldMatch open

        body.close()

        streams shouldMatch closed
    }

    @Test
    fun `closing streams - manually created multipart can be closed via its stream`() {
        val streams = (1..3).map { "content $it" }.map { TestInputStream(it) }

        val body = streams.toMultipartForm()

        streams shouldMatch open

        body.stream.close()

        streams shouldMatch closed
    }

    @Test
    fun `closing streams - parsed from existing message`() {
        val streams = (1..3).map { "content $it" }.map { TestInputStream(it) }

        val original = streams.toMultipartForm()

        MultipartFormBody.from(Request(Method.POST, "/")
            .with(Header.Common.CONTENT_TYPE of ContentType.MultipartFormWithBoundary(original.boundary))
            .body(original))

        streams shouldMatch closed //original stream are automatically closed during parsing
    }

    private fun List<TestInputStream>.toMultipartForm() =
        foldIndexed(MultipartFormBody())
        { index, acc, stream -> acc.plus("file$index" to FormFile("foo$index.txt", ContentType.TEXT_PLAIN, stream)) }

    private class TestInputStream(private var text: String) : InputStream() {
        private val stream = text.byteInputStream()
        var closed: Boolean = false

        override fun read(): Int = stream.read()

        override fun close() {
            super.close()
            closed = true
        }

        override fun toString(): String = "($closed) ${text.substring(0, 9)}"
    }

    private val isClosed = Matcher(TestInputStream::closed)
    private val open = allElements(!isClosed)
    private val closed = allElements(isClosed)
}
