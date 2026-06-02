package org.http4k.core

import com.natpryce.hamkrest.Matcher
import com.natpryce.hamkrest.allElements
import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.equalTo
import org.http4k.core.ContentType.Companion.OCTET_STREAM
import org.http4k.core.ContentType.Companion.TEXT_PLAIN
import org.http4k.core.Method.POST
import org.http4k.lens.Header
import org.http4k.lens.MultipartFormField
import org.http4k.lens.MultipartFormFile
import org.http4k.multipart.DiskLocation
import org.http4k.multipart.MultipartFormBuilder
import org.http4k.multipart.ParseError
import org.http4k.multipart.StreamTooLongException
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertThrows
import org.junit.jupiter.api.Test
import java.io.InputStream
import kotlin.io.path.createTempDirectory

class MultipartFormBodyTest {

    @Test
    fun `retrieving files and fields`() {
        val file1 = MultipartFormFile("foo.txt", TEXT_PLAIN, "content".byteInputStream())
        val file2 = MultipartFormFile("foo2.txt", TEXT_PLAIN, "content2".byteInputStream())
        val form = MultipartFormBody("bob") +
            ("field" to "foo") +
            ("field" to "bar") +
            ("file" to file1) +
            ("file" to file2)

        assertThat(form.fieldValue("field"), equalTo("foo"))
        assertThat(form.fieldValues("field"), equalTo(listOf("foo", "bar")))

        assertThat(form.file("file"), equalTo(file1))
        assertThat(form.files("file"), equalTo(listOf(file1, file2)))
    }

    private val formFieldValue = MultipartFormField("bar", listOf("Content-Disposition" to """form-data; name="field""""))

    @Test
    fun roundtrip() {
        val form = MultipartFormBody("bob") + ("field" to formFieldValue) +
            ("file" to MultipartFormFile("foo.txt", TEXT_PLAIN, "content".byteInputStream()))

        val req = Request(POST, "")
            .with(Header.CONTENT_TYPE of ContentType.MultipartFormWithBoundary(form.boundary))
            .body(form)

        assertThat(
            MultipartFormBody.from(req), equalTo(
                MultipartFormBody("bob") + ("field" to formFieldValue) +
                    ("file" to MultipartFormFile("foo.txt", TEXT_PLAIN, "content".byteInputStream()))
            )
        )
    }

    @Test
    fun `can handle when body is already pulled into memory`() {
        val form = MultipartFormBody("bob") + ("field" to formFieldValue) +
            ("file" to MultipartFormFile("foo.txt", TEXT_PLAIN, "content".byteInputStream()))

        val req = Request(POST, "")
            .with(Header.CONTENT_TYPE of ContentType.MultipartFormWithBoundary(form.boundary))
            .body(form)

        req.bodyString()

        assertThat(MultipartFormBody.from(req), equalTo(
            MultipartFormBody("bob") + ("field" to formFieldValue) +
                ("file" to MultipartFormFile("foo.txt", TEXT_PLAIN, "content".byteInputStream()))
        ))
    }

    @Test
    fun `closing streams - manually created multipart`() {
        val streams = (1..3).map { "content $it" }.map { TestInputStream(it) }

        val body = streams.toMultipartForm()

        assertThat(streams, open)

        body.close()

        assertThat(streams, closed)
    }

    @Test
    fun `closing streams - manually created multipart can be closed via its stream`() {
        val streams = (1..3).map { "content $it" }.map { TestInputStream(it) }

        val body = streams.toMultipartForm()

        assertThat(streams, open)

        body.stream.close()

        assertThat(streams, closed)
    }

    @Test
    fun `closing streams - parsed from existing message`() {
        val streams = (1..3).map { "content $it" }.map { TestInputStream(it) }

        val original = streams.toMultipartForm()

        MultipartFormBody.from(Request(POST, "/")
            .with(Header.CONTENT_TYPE of ContentType.MultipartFormWithBoundary(original.boundary))
            .body(original))

        assertThat(streams, closed)
        //original stream are automatically closed during parsing
    }

    @Test
    fun `gets boundary successfully from content type`() {
        val boundary = "boundary"

        val withCharset = Request(POST, "")
            .with(Header.CONTENT_TYPE of ContentType("multipart/form-data", listOf("charset" to "utf-8", "boundary" to boundary)))
            .body(MultipartFormBody(boundary))

        val noCharset = Request(POST, "")
            .with(Header.CONTENT_TYPE of ContentType.MultipartFormWithBoundary(boundary))
            .body(MultipartFormBody(boundary))

        assertThat(MultipartFormBody.from(withCharset).boundary, equalTo(boundary))
        assertThat(MultipartFormBody.from(noCharset).boundary, equalTo(boundary))
    }

    @Test
    fun `file part without content type defaults to octet stream`() {
        val boundary = "bob"
        val body = MultipartFormBuilder(boundary)
            .part(
                "file content".byteInputStream(),
                listOf("Content-Disposition" to """form-data; name="file"; filename="test.bin"""")
            )
            .stream()
        val req = Request(POST, "")
            .with(Header.CONTENT_TYPE of ContentType.MultipartFormWithBoundary(boundary))
            .body(Body(body))
        val parsed = MultipartFormBody.from(req)
        assertThat(parsed.file("file")?.contentType, equalTo(OCTET_STREAM))
    }

    @Test
    fun `multipartIterator file part without content type defaults to octet stream`() {
        val boundary = "bob"
        val body = MultipartFormBuilder(boundary)
            .part(
                "file content".byteInputStream(),
                listOf("Content-Disposition" to """form-data; name="file"; filename="test.bin"""")
            )
            .stream()
        val req = Request(POST, "")
            .with(Header.CONTENT_TYPE of ContentType.MultipartFormWithBoundary(boundary))
            .body(Body(body))
        val entity = req.multipartIterator().asSequence().first()
        assertThat((entity as MultipartEntity.File).file.contentType, equalTo(OCTET_STREAM))
    }

    @Test
    fun `from cleans up the DiskLocation when parsing throws`() {
        val diskDir = createTempDirectory().toFile()
        val boundary = "bob"
        val oversizedField = "x".repeat(11 * 1024 * 1024)
        val body = MultipartFormBuilder(boundary)
            .field("big", oversizedField, listOf("Content-Disposition" to """form-data; name="big""""))
            .stream()
        val req = Request(POST, "")
            .with(Header.CONTENT_TYPE of ContentType.MultipartFormWithBoundary(boundary))
            .body(Body(body))

        assertThrows(StreamTooLongException::class.java) {
            MultipartFormBody.from(req, diskLocation = DiskLocation.Temp(diskDir))
        }
        assertFalse(diskDir.exists(), "temp dir should be removed on parse failure")
    }

    @Test
    fun `from rejects a body larger than the configured stream limit`() {
        val boundary = "bob"
        val oversizedField = "x".repeat(11 * 1024 * 1024)
        val body = MultipartFormBuilder(boundary)
            .field("big", oversizedField, listOf("Content-Disposition" to """form-data; name="big""""))
            .stream()
        val req = Request(POST, "")
            .with(Header.CONTENT_TYPE of ContentType.MultipartFormWithBoundary(boundary))
            .body(Body(body))

        assertThrows(StreamTooLongException::class.java) { MultipartFormBody.from(req) }
    }

    @Test
    fun `from rejects a body containing more parts than the configured part limit`() {
        val boundary = "bob"
        val builder = MultipartFormBuilder(boundary)
        repeat(1001) { i ->
            builder.field("f$i", "v", listOf("Content-Disposition" to """form-data; name="f$i""""))
        }
        val req = Request(POST, "")
            .with(Header.CONTENT_TYPE of ContentType.MultipartFormWithBoundary(boundary))
            .body(Body(builder.stream()))

        assertThrows(ParseError::class.java) { MultipartFormBody.from(req) }
    }

    @Test
    fun `multipartIterator rejects a body containing more parts than the configured part limit`() {
        val boundary = "bob"
        val builder = MultipartFormBuilder(boundary)
        repeat(1001) { i ->
            builder.field("f$i", "v", listOf("Content-Disposition" to """form-data; name="f$i""""))
        }
        val req = Request(POST, "")
            .with(Header.CONTENT_TYPE of ContentType.MultipartFormWithBoundary(boundary))
            .body(Body(builder.stream()))

        assertThrows(ParseError::class.java) { req.multipartIterator().asSequence().toList() }
    }

    private fun List<TestInputStream>.toMultipartForm() =
        foldIndexed(MultipartFormBody())
        { index, acc, stream -> acc.plus("file$index" to MultipartFormFile("foo$index.txt", TEXT_PLAIN, stream)) }

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
