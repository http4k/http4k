package org.http4k.multipart

import org.junit.jupiter.api.Test
import java.nio.charset.Charset
import java.nio.charset.StandardCharsets

class StreamingMultipartFormEncodingTests {

    @Test
    fun uploadUTF8() {
        testForm(constructForm(StandardCharsets.UTF_8), "\u00E9", "\uD83D\uDCA9", StandardCharsets.UTF_8)
    }

    @Test
    fun uploadISO_8859_1() {
        testForm(constructForm(StandardCharsets.ISO_8859_1), "\u00E9", "?", StandardCharsets.ISO_8859_1)
    }

    @Test
    fun uploadUTF_16BE() {
        testForm(constructForm(StandardCharsets.UTF_16BE), "\u00E9", "\uD83D\uDCA9", StandardCharsets.UTF_16BE)
    }

    @Test
    fun uploadUTF_16LE() {
        testForm(constructForm(StandardCharsets.UTF_16LE), "\u00E9", "\uD83D\uDCA9", StandardCharsets.UTF_16LE)
    }

    @Test
    fun uploadUS_ASCII() {
        testForm(constructForm(StandardCharsets.US_ASCII), "?", "?", StandardCharsets.US_ASCII)
    }

    private fun testForm(form: Iterator<StreamingPart>, simpleChar: String, complexChar: String, encoding: Charset) {
        assertFilePart(form, "file", "foo.tab$complexChar", "text/whatever$simpleChar$complexChar", "This is the content of the file$simpleChar$complexChar", encoding)
        assertFieldPart(form, "field$complexChar", "fieldValue$simpleChar$complexChar", encoding)
        assertFieldPart(form, "multi", "value1$simpleChar", encoding)
        assertFilePart(form, "anotherFile", "BAR.tab", "text/something$simpleChar", "This is another file$simpleChar", encoding)
        assertFieldPart(form, "multi", "value2$simpleChar", encoding)

        assertThereAreNoMoreParts(form)
    }

    private fun constructForm(encoding: Charset): Iterator<StreamingPart> {
        val boundary = "-----\u00E91234\uD83D\uDCA9"
        val boundaryBytes = boundary.toByteArray(encoding)
        return getMultipartFormParts(boundaryBytes,
            MultipartFormBuilder(boundaryBytes, encoding)
                .file("file", "foo.tab\uD83D\uDCA9", "text/whatever\u00E9\uD83D\uDCA9", "This is the content of the file\u00E9\uD83D\uDCA9".byteInputStream(encoding), emptyList())
                .field("field\uD83D\uDCA9", "fieldValue\u00E9\uD83D\uDCA9", emptyList())
                .field("multi", "value1\u00E9", emptyList())
                .file("anotherFile", "BAR.tab", "text/something\u00E9", "This is another file\u00E9".byteInputStream(encoding), emptyList())
                .field("multi", "value2\u00E9", emptyList())
                .stream(), encoding)
    }
}
