package org.http4k.multipart

import com.natpryce.hamkrest.absent
import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.equalTo
import com.natpryce.hamkrest.has
import com.natpryce.hamkrest.present
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.fail

class StreamingMultipartFormSadTests {

    @Test
    fun `fails when no boundary in stream`() {
        val boundary = "---1234"
        var form = getMultipartFormParts(boundary, "No boundary anywhere".byteInputStream())

        assertParseErrorWrapsTokenNotFound(form, "Boundary not found <<-----1234>>")

        form = getMultipartFormParts(boundary, "No boundary anywhere".byteInputStream())

        try {
            form.next()
            fail("Should have thrown ParseError")
        } catch (e: ParseError) {
            assertThat(e.cause!!, has(Throwable::message, present(equalTo("Boundary not found <<-----1234>>"))))
        }
    }

    @Test
    fun `fails when getting next past end of parts`() {
        val boundary = "-----1234"
        val form = getMultipartFormParts(boundary, MultipartFormBuilder(boundary)
            .file("aFile", "file.name", "application/octet-stream", "File contents here".byteInputStream(), emptyList())
            .file("anotherFile", "your.name", "application/octet-stream", "Different file contents here".byteInputStream(), emptyList()).stream())

        form.next() // aFile
        form.next() // anotherFile
        try {
            form.next() // no such element
            fail("Should have thrown NoSuchElementException")
        } catch (e: NoSuchElementException) {
            // pass
        }
    }

    @Test
    fun `fails when getting next past end of parts after has next`() {
        val boundary = "-----1234"
        val form = getMultipartFormParts(boundary, MultipartFormBuilder(boundary)
            .file("aFile", "file.name", "application/octet-stream", "File contents here".byteInputStream(), emptyList())
            .file("anotherFile", "your.name", "application/octet-stream", "Different file contents here".byteInputStream(), emptyList()).stream())

        form.next() // aFile
        form.next() // anotherFile
        assertThat(form.hasNext(), equalTo(false))
        try {
            form.next() // no such element
            fail("Should have thrown NoSuchElementException")
        } catch (e: NoSuchElementException) {
            // pass
        }
    }

    @Test
    @Disabled("this is not a valid test case according to the RFC - we should blow up..")
    fun `part has no headers`() {
        val boundary = "-----2345"
        val form = getMultipartFormParts(boundary, MultipartFormBuilder(boundary)
            .field("multi", "value0", emptyList())
            .part("" + CR_LF + "value with no headers", emptyList())
            .field("multi", "value2", emptyList())
            .stream())

        form.next()
        val StreamingPart = form.next()
        assertThat(StreamingPart.fieldName, absent())
        assertThat(StreamingPart.contentsAsString, equalTo("value with no headers"))
        assertThat(StreamingPart.headers.size, equalTo(0))
        assertThat(StreamingPart.isFormField, equalTo(true))
        assertThat(StreamingPart.fileName, absent())
        form.next()
    }

    @Test
    fun `overwrites part header if header is repeated`() {
        val boundary = "-----2345"
        val form = getMultipartFormParts(boundary, MultipartFormBuilder(boundary)
            .part("contents of StreamingPart",
                listOf("Content-Disposition" to "form-data; bit=\"first\"; name=\"first-name\"",
                    "Content-Disposition" to "form-data; bot=\"second\"; name=\"second-name\""))
            .stream())

        val StreamingPart = form.next()
        assertThat(StreamingPart.fieldName, equalTo("second-name"))
        assertThat(StreamingPart.headers["Content-Disposition"],
            equalTo("form-data; bot=\"second\"; name=\"second-name\""))
    }

    @Test
    fun `fails if found boundary but no field separator`() {
        val boundary = "---2345"

        val form = getMultipartFormParts(boundary, ("-----2345" + // no CR_LF

            "Content-Disposition: form-data; name=\"name\"" + CR_LF +
            "" + CR_LF +
            "value" + CR_LF +
            "-----2345--" + CR_LF).byteInputStream())

        assertParseErrorWrapsTokenNotFound(form, "Boundary must be followed by field separator, but didn't find it")
    }

    @Test
    fun `fails if header missing field separator`() {
        val boundary = "---2345"

        assertParseError(getMultipartFormParts(boundary, ("-----2345" + CR_LF +
            "Content-Disposition: form-data; name=\"name\"" + // no CR_LF

            "" + CR_LF +
            "value" + CR_LF +
            "-----2345--" + CR_LF).byteInputStream()), "Header didn't include a colon <<value>>")

        assertParseError(getMultipartFormParts(boundary, ("-----2345" + CR_LF +
            "Content-Disposition: form-data; name=\"name\"" + CR_LF +
            // no CR_LF
            "value" + CR_LF +
            "-----2345--" + CR_LF).byteInputStream()), "Header didn't include a colon <<value>>")
    }

    @Test
    fun `fails if contents missing field separator`() {
        val boundary = "---2345"

        val form = getMultipartFormParts(boundary, ("-----2345" + CR_LF +
            "Content-Disposition: form-data; name=\"name\"" + CR_LF +
            "" + CR_LF +
            "value" + // no CR_LF

            "-----2345--" + CR_LF).byteInputStream())

        form.next()
        // StreamingPart's content stream hasn't been closed
        assertParseErrorWrapsTokenNotFound(form, "Boundary must be proceeded by field separator, but didn't find it")
    }

    @Test
    fun `fails if contents missing field separator and has read to end of content`() {
        val boundary = "---2345"

        val form = getMultipartFormParts(boundary, ("-----2345" + CR_LF +
            "Content-Disposition: form-data; name=\"name\"" + CR_LF +
            "" + CR_LF +
            "value" + // no CR_LF

            "-----2345--" + CR_LF).byteInputStream())

        val StreamingPart = form.next()
        StreamingPart.contentsAsString
        assertParseErrorWrapsTokenNotFound(form, "Boundary must be proceeded by field separator, but didn't find it")
    }

    @Test
    fun `fails if closing boundary is missing field separator`() {
        val boundary = "---2345"

        val form = getMultipartFormParts(boundary, ("-----2345" + CR_LF +
            "Content-Disposition: form-data; name=\"name\"" + CR_LF +
            "" + CR_LF +
            "value" + CR_LF +
            "-----2345--").byteInputStream()) // no CR_LF

        form.next()
        assertParseErrorWrapsTokenNotFound(form, "Stream terminator must be followed by field separator, but didn't find it")
    }

    @Test
    fun `fails if closing boundary is missing`() {
        val boundary = "---2345"

        val form = getMultipartFormParts(boundary, ("-----2345" + CR_LF +
            "Content-Disposition: form-data; name=\"name\"" + CR_LF +
            "" + CR_LF +
            "value" + CR_LF +
            "-----2345" + CR_LF).byteInputStream())

        form.next()
        assertParseErrorWrapsTokenNotFound(form, "Reached end of stream before finding Token <<\r\n>>. Last 2 bytes read were <<>>")
    }

    @Test
    fun `fails if heading too long`() {
        val boundary = "---2345"

        val chars = CharArray(StreamingMultipartFormParts.HEADER_SIZE_MAX)
        chars.fill('x')
        val form = getMultipartFormParts(boundary, MultipartFormBuilder(boundary)
            .file("aFile", String(chars), "application/octet-stream", "File contents here".byteInputStream(), emptyList()).stream())

        assertParseErrorWrapsTokenNotFound(form, "Didn't find end of Token <<\r\n>> within 10240 bytes")
    }

    @Test
    fun `fails if too many headings`() {
        val boundary = "---2345"

        val chars = CharArray(1024)
        chars.fill('x')
        val form = getMultipartFormParts(boundary, MultipartFormBuilder(boundary)
            .part("some contents",
                listOf("Content-Disposition" to "form-data; name=\"fieldName\"; filename=\"filename\"",
                    "Content-Type" to "text/plain",
                    "extra-1" to String(chars),
                    "extra-2" to String(chars),
                    "extra-3" to String(chars),
                    "extra-4" to String(chars),
                    "extra-5" to String(chars),
                    "extra-6" to String(chars),
                    "extra-7" to String(chars),
                    "extra-8" to String(chars),
                    "extra-9" to String(chars),
                    "extra-10" to String(chars, 0, 816)) // header section exactly 10240 bytes big!
            ).stream())

        assertParseErrorWrapsTokenNotFound(form, "Didn't find end of Header section within 10240 bytes")
    }

    private fun assertParseErrorWrapsTokenNotFound(form: Iterator<StreamingPart>, errorMessage: String) {
        try {
            form.hasNext()
        } catch (e: ParseError) {
            assertThat(e.cause!!, has(Throwable::message, present(equalTo(errorMessage))))
        }
    }

    private fun assertParseError(form: Iterator<StreamingPart>, errorMessage: String) {
        try {
            form.hasNext() // will hit missing \r\n
            fail("Should have thrown a parse error")
        } catch (e: ParseError) {
            assertThat(e.message, equalTo(errorMessage))
        }
    }
}
