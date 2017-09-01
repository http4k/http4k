package org.http4k.multipart

import com.natpryce.hamkrest.absent
import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.equalTo
import com.natpryce.hamkrest.has
import com.natpryce.hamkrest.present
import org.http4k.multipart.exceptions.ParseError
import org.http4k.multipart.part.StreamingPart
import org.junit.Assert.fail
import org.junit.Test
import java.util.*

class StreamingMultipartFormSadTests {

    @Test
    fun failsWhenNoBoundaryInStream() {
        val boundary = "---1234"
        var form = StreamingMultipartFormHappyTests.getMultipartFormParts(boundary, "No boundary anywhere".toByteArray())

        assertParseErrorWrapsTokenNotFound(form, "Boundary not found <<-----1234>>")

        form = StreamingMultipartFormHappyTests.getMultipartFormParts(boundary, "No boundary anywhere".toByteArray())

        try {
            form.next()
            fail("Should have thrown ParseError")
        } catch (e: ParseError) {
            assertThat(e.cause!!, has(Throwable::message, present(equalTo("Boundary not found <<-----1234>>"))))
        }

    }

    @Test
    fun failsWhenGettingNextPastEndOfParts() {
        val boundary = "-----1234"
        val form = StreamingMultipartFormHappyTests.getMultipartFormParts(boundary, ValidMultipartFormBuilder(boundary)
            .file("aFile", "file.name", "application/octet-stream", "File contents here")
            .file("anotherFile", "your.name", "application/octet-stream", "Different file contents here").build())

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
    fun failsWhenGettingNextPastEndOfPartsAfterHasNext() {
        val boundary = "-----1234"
        val form = StreamingMultipartFormHappyTests.getMultipartFormParts(boundary, ValidMultipartFormBuilder(boundary)
            .file("aFile", "file.name", "application/octet-stream", "File contents here")
            .file("anotherFile", "your.name", "application/octet-stream", "Different file contents here").build())

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
    fun partHasNoHeaders() {
        val boundary = "-----2345"
        val form = StreamingMultipartFormHappyTests.getMultipartFormParts(boundary, ValidMultipartFormBuilder(boundary)
            .field("multi", "value0")
            .rawPart("" + StreamingMultipartFormHappyTests.CR_LF + "value with no headers")
            .field("multi", "value2")
            .build())

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
    fun overwritesPartHeaderIfHeaderIsRepeated() {
        val boundary = "-----2345"
        val form = StreamingMultipartFormHappyTests.getMultipartFormParts(boundary, ValidMultipartFormBuilder(boundary)
            .part("contents of StreamingPart",
                Pair("Content-Disposition", listOf(Pair("form-data", null), Pair("bit", "first"), Pair("name", "first-name"))),
                Pair("Content-Disposition", listOf(Pair("form-data", null), Pair("bot", "second"), Pair("name", "second-name"))))
            .build())

        val StreamingPart = form.next()
        assertThat(StreamingPart.fieldName, equalTo("second-name"))
        assertThat(StreamingPart.headers["Content-Disposition"],
            equalTo("form-data; bot=\"second\"; name=\"second-name\""))
    }

    @Test
    fun failsIfFoundBoundaryButNoFieldSeparator() {
        val boundary = "---2345"

        val form = StreamingMultipartFormHappyTests.getMultipartFormParts(boundary, ("-----2345" + // no CR_LF

            "Content-Disposition: form-data; name=\"name\"" + StreamingMultipartFormHappyTests.CR_LF +
            "" + StreamingMultipartFormHappyTests.CR_LF +
            "value" + StreamingMultipartFormHappyTests.CR_LF +
            "-----2345--" + StreamingMultipartFormHappyTests.CR_LF).toByteArray())

        assertParseErrorWrapsTokenNotFound(form, "Boundary must be followed by field separator, but didn't find it")
    }

    @Test
    fun failsIfHeaderMissingFieldSeparator() {
        val boundary = "---2345"

        assertParseError(StreamingMultipartFormHappyTests.getMultipartFormParts(boundary, ("-----2345" + StreamingMultipartFormHappyTests.CR_LF +
            "Content-Disposition: form-data; name=\"name\"" + // no CR_LF

            "" + StreamingMultipartFormHappyTests.CR_LF +
            "value" + StreamingMultipartFormHappyTests.CR_LF +
            "-----2345--" + StreamingMultipartFormHappyTests.CR_LF).toByteArray()), "Header didn't include a colon <<value>>")


        assertParseError(StreamingMultipartFormHappyTests.getMultipartFormParts(boundary, ("-----2345" + StreamingMultipartFormHappyTests.CR_LF +
            "Content-Disposition: form-data; name=\"name\"" + StreamingMultipartFormHappyTests.CR_LF +
            // no CR_LF
            "value" + StreamingMultipartFormHappyTests.CR_LF +
            "-----2345--" + StreamingMultipartFormHappyTests.CR_LF).toByteArray()), "Header didn't include a colon <<value>>")
    }

    @Test
    fun failsIfContentsMissingFieldSeparator() {
        val boundary = "---2345"

        val form = StreamingMultipartFormHappyTests.getMultipartFormParts(boundary, ("-----2345" + StreamingMultipartFormHappyTests.CR_LF +
            "Content-Disposition: form-data; name=\"name\"" + StreamingMultipartFormHappyTests.CR_LF +
            "" + StreamingMultipartFormHappyTests.CR_LF +
            "value" + // no CR_LF

            "-----2345--" + StreamingMultipartFormHappyTests.CR_LF).toByteArray())

        form.next()
        // StreamingPart's content stream hasn't been closed
        assertParseErrorWrapsTokenNotFound(form, "Boundary must be proceeded by field separator, but didn't find it")
    }

    @Test
    fun failsIfContentsMissingFieldSeparatorAndHasReadToEndOfContent() {
        val boundary = "---2345"

        val form = StreamingMultipartFormHappyTests.getMultipartFormParts(boundary, ("-----2345" + StreamingMultipartFormHappyTests.CR_LF +
            "Content-Disposition: form-data; name=\"name\"" + StreamingMultipartFormHappyTests.CR_LF +
            "" + StreamingMultipartFormHappyTests.CR_LF +
            "value" + // no CR_LF

            "-----2345--" + StreamingMultipartFormHappyTests.CR_LF).toByteArray())

        val StreamingPart = form.next()
        StreamingPart.contentsAsString
        assertParseErrorWrapsTokenNotFound(form, "Boundary must be proceeded by field separator, but didn't find it")
    }

    @Test
    fun failsIfClosingBoundaryIsMissingFieldSeparator() {
        val boundary = "---2345"

        val form = StreamingMultipartFormHappyTests.getMultipartFormParts(boundary, ("-----2345" + StreamingMultipartFormHappyTests.CR_LF +
            "Content-Disposition: form-data; name=\"name\"" + StreamingMultipartFormHappyTests.CR_LF +
            "" + StreamingMultipartFormHappyTests.CR_LF +
            "value" + StreamingMultipartFormHappyTests.CR_LF +
            "-----2345--").toByteArray()) // no CR_LF

        form.next()
        assertParseErrorWrapsTokenNotFound(form, "Stream terminator must be followed by field separator, but didn't find it")
    }

    @Test
    fun failsIfClosingBoundaryIsMissing() {
        val boundary = "---2345"

        val form = StreamingMultipartFormHappyTests.getMultipartFormParts(boundary, ("-----2345" + StreamingMultipartFormHappyTests.CR_LF +
            "Content-Disposition: form-data; name=\"name\"" + StreamingMultipartFormHappyTests.CR_LF +
            "" + StreamingMultipartFormHappyTests.CR_LF +
            "value" + StreamingMultipartFormHappyTests.CR_LF +
            "-----2345" + StreamingMultipartFormHappyTests.CR_LF).toByteArray())

        form.next()
        assertParseErrorWrapsTokenNotFound(form, "Reached end of stream before finding Token <<\r\n>>. Last 2 bytes read were <<>>")
    }

    @Test
    fun failsIfHeadingTooLong() {
        val boundary = "---2345"

        val chars = CharArray(StreamingMultipartFormParts.HEADER_SIZE_MAX)
        chars.fill('x')
        val form = StreamingMultipartFormHappyTests.getMultipartFormParts(boundary, ValidMultipartFormBuilder(boundary)
            .file("aFile", String(chars), "application/octet-stream", "File contents here").build())

        assertParseErrorWrapsTokenNotFound(form, "Didn't find end of Token <<\r\n>> within 10240 bytes")
    }

    @Test
    fun failsIfTooManyHeadings() {
        val boundary = "---2345"

        val chars = CharArray(1024)
        chars.fill('x')
        val form = StreamingMultipartFormHappyTests.getMultipartFormParts(boundary, ValidMultipartFormBuilder(boundary)
            .part("some contents",
                Pair("Content-Disposition", listOf(Pair("form-data", null), Pair("name", "fieldName"), Pair("filename", "filename"))),
                Pair("Content-Type", listOf(Pair("text/plain", null))),
                Pair("extra-1", listOf(Pair(String(chars), null))),
                Pair("extra-2", listOf(Pair(String(chars), null))),
                Pair("extra-3", listOf(Pair(String(chars), null))),
                Pair("extra-4", listOf(Pair(String(chars), null))),
                Pair("extra-5", listOf(Pair(String(chars), null))),
                Pair("extra-6", listOf(Pair(String(chars), null))),
                Pair("extra-7", listOf(Pair(String(chars), null))),
                Pair("extra-8", listOf(Pair(String(chars), null))),
                Pair("extra-9", listOf(Pair(String(chars), null))),
                Pair("extra-10", listOf(Pair(String(chars, 0, 816), null))) // header section exactly 10240 bytes big!
            ).build())

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
