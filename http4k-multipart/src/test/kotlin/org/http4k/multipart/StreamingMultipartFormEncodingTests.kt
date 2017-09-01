package org.http4k.multipart

import org.http4k.multipart.part.StreamingPart
import org.junit.Test

import java.io.IOException
import java.nio.charset.Charset
import java.nio.charset.StandardCharsets

class StreamingMultipartFormEncodingTests {

    @Test
    @Throws(Exception::class)
    fun uploadUTF8() {
        testForm(constructForm(StandardCharsets.UTF_8), "\u00E9", "\uD83D\uDCA9", StandardCharsets.UTF_8)
    }

    @Test
    @Throws(Exception::class)
    fun uploadISO_8859_1() {
        testForm(constructForm(StandardCharsets.ISO_8859_1), "\u00E9", "?", StandardCharsets.ISO_8859_1)
    }

    @Test
    @Throws(Exception::class)
    fun uploadUTF_16BE() {
        testForm(constructForm(StandardCharsets.UTF_16BE), "\u00E9", "\uD83D\uDCA9", StandardCharsets.UTF_16BE)
    }

    @Test
    @Throws(Exception::class)
    fun uploadUTF_16LE() {
        testForm(constructForm(StandardCharsets.UTF_16LE), "\u00E9", "\uD83D\uDCA9", StandardCharsets.UTF_16LE)
    }

    @Test
    @Throws(Exception::class)
    fun uploadUS_ASCII() {
        testForm(constructForm(StandardCharsets.US_ASCII), "?", "?", StandardCharsets.US_ASCII)
    }

    @Throws(IOException::class)
    private fun testForm(form: Iterator<StreamingPart>, simpleChar: String, complexChar: String, encoding: Charset) {
        StreamingMultipartFormHappyTests.assertFilePart(form, "file", "foo.tab" + complexChar, "text/whatever" + simpleChar + complexChar, "This is the content of the file" + simpleChar + complexChar, encoding)
        StreamingMultipartFormHappyTests.assertFieldPart(form, "field" + complexChar, "fieldValue" + simpleChar + complexChar, encoding)
        StreamingMultipartFormHappyTests.assertFieldPart(form, "multi", "value1" + simpleChar, encoding)
        StreamingMultipartFormHappyTests.assertFilePart(form, "anotherFile", "BAR.tab", "text/something" + simpleChar, "This is another file" + simpleChar, encoding)
        StreamingMultipartFormHappyTests.assertFieldPart(form, "multi", "value2" + simpleChar, encoding)

        StreamingMultipartFormHappyTests.assertThereAreNoMoreParts(form)
    }

    @Throws(IOException::class)
    private fun constructForm(encoding: Charset): Iterator<StreamingPart> {
        val boundary = "-----\u00E91234\uD83D\uDCA9"
        val boundaryBytes = boundary.toByteArray(encoding)
        return StreamingMultipartFormHappyTests.getMultipartFormParts(boundaryBytes,
            ValidMultipartFormBuilder(boundaryBytes, encoding)
                .file("file", "foo.tab\uD83D\uDCA9", "text/whatever\u00E9\uD83D\uDCA9", "This is the content of the file\u00E9\uD83D\uDCA9")
                .field("field\uD83D\uDCA9", "fieldValue\u00E9\uD83D\uDCA9")
                .field("multi", "value1\u00E9")
                .file("anotherFile", "BAR.tab", "text/something\u00E9", "This is another file\u00E9")
                .field("multi", "value2\u00E9")
                .build(), encoding)
    }
}
