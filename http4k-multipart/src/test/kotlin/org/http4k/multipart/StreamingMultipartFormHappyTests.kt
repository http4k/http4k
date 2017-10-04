package org.http4k.multipart

import org.hamcrest.CoreMatchers.equalTo
import org.hamcrest.MatcherAssert.assertThat
import org.hamcrest.core.IsNot.not
import org.http4k.multipart.part.StreamingPart
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test
import java.io.ByteArrayInputStream
import java.io.FileInputStream
import java.io.InputStream
import java.nio.charset.Charset
import java.nio.charset.StandardCharsets

class StreamingMultipartFormHappyTests {

    @Test
    fun uploadEmptyContents() {
        val boundary = "-----1234"
        val form = getMultipartFormParts(boundary, MultipartFormBuilder(boundary).build())

        assertThereAreNoMoreParts(form)
    }

    @Test
    fun uploadEmptyFile() {
        val boundary = "-----2345"
        val form = getMultipartFormParts(boundary, MultipartFormBuilder(boundary)
            .file("aFile", "", "doesnt/matter", "".byteInputStream()).build())

        assertFilePart(form, "aFile", "", "doesnt/matter", "")

        assertThereAreNoMoreParts(form)
    }

    @Test
    fun hasNextIsIdempotent() {
        val boundary = "-----2345"
        val form = getMultipartFormParts(boundary, MultipartFormBuilder(boundary)
            .file("aFile", "", "application/octet-stream", "".byteInputStream())
            .file("anotherFile", "", "application/octet-stream", "".byteInputStream()).build())

        assertThereAreMoreParts(form)
        assertThereAreMoreParts(form)

        form.next()

        assertThereAreMoreParts(form)
        assertThereAreMoreParts(form)

        form.next()

        assertThereAreNoMoreParts(form)
        assertThereAreNoMoreParts(form)
    }

    @Test
    fun uploadEmptyField() {
        val boundary = "-----3456"
        val form = getMultipartFormParts(boundary, MultipartFormBuilder(boundary)
            .field("aField", "").build())

        assertFieldPart(form, "aField", "")

        assertThereAreNoMoreParts(form)
    }

    @Test
    fun uploadSmallFile() {
        val boundary = "-----2345"
        val form = getMultipartFormParts(boundary, MultipartFormBuilder(boundary)
            .file("aFile", "file.name", "application/octet-stream", "File contents here".byteInputStream()).build())

        assertFilePart(form, "aFile", "file.name", "application/octet-stream", "File contents here")

        assertThereAreNoMoreParts(form)
    }

    @Test
    fun uploadSmallFileAsAttachment() {
        val boundary = "-----4567"
        val form = getMultipartFormParts(boundary, MultipartFormBuilder(boundary)
            .file("beforeFile", "before.txt", "application/json", "[]".byteInputStream())
            .startMultipart("multipartFieldName", "7890")
            .attachment("during.txt", "plain/text", "Attachment contents here")
            .attachment("during2.txt", "plain/text", "More text here")
            .endMultipart()
            .file("afterFile", "after.txt", "application/json", "[]".byteInputStream())
            .build())

        assertFilePart(form, "beforeFile", "before.txt", "application/json", "[]")
        assertFilePart(form, "multipartFieldName", "during.txt", "plain/text", "Attachment contents here")
        assertFilePart(form, "multipartFieldName", "during2.txt", "plain/text", "More text here")
        assertFilePart(form, "afterFile", "after.txt", "application/json", "[]")

        assertThereAreNoMoreParts(form)
    }


    @Test
    fun uploadSmallField() {
        val boundary = "-----3456"
        val form = getMultipartFormParts(boundary, MultipartFormBuilder(boundary)
            .field("aField", "Here is the value of the field\n").build())

        assertFieldPart(form, "aField", "Here is the value of the field\n")

        assertThereAreNoMoreParts(form)
    }

    @Test
    fun uploadMultipleFilesAndFields() {
        val boundary = "-----1234"
        val form = getMultipartFormParts(boundary,
            MultipartFormBuilder(boundary)
                .file("file", "foo.tab", "text/whatever", "This is the content of the file\n".byteInputStream())
                .field("field", "fieldValue" + CR_LF + "with cr lf")
                .field("multi", "value1")
                .file("anotherFile", "BAR.tab", "text/something", "This is another file\n".byteInputStream())
                .field("multi", "value2")
                .build())

        assertFilePart(form, "file", "foo.tab", "text/whatever", "This is the content of the file\n")
        assertFieldPart(form, "field", "fieldValue" + CR_LF + "with cr lf")
        assertFieldPart(form, "multi", "value1")
        assertFilePart(form, "anotherFile", "BAR.tab", "text/something", "This is another file\n")
        assertFieldPart(form, "multi", "value2")

        assertThereAreNoMoreParts(form)
    }

    @Test
    fun uploadFieldsWithMultilineHeaders() {
        val boundary = "-----1234"
        val form = getMultipartFormParts(boundary,
            MultipartFormBuilder(boundary)
                .part("Content-Disposition: form-data; \r\n" +
                    "\tname=\"field\"\r\n" +
                    "\r\n" +
                    "fieldValue")
                .part("Content-Disposition: form-data;\r\n" +
                    "     name=\"multi\"\r\n" +
                    "\r\n" +
                    "value1")
                .field("multi", "value2")
                .build())

        assertFieldPart(form, "field", "fieldValue")
        assertFieldPart(form, "multi", "value1")
        assertFieldPart(form, "multi", "value2")

        assertThereAreNoMoreParts(form)
    }

    @Test
    fun partsCanHaveLotsOfHeaders() {
        val boundary = "-----1234"
        val form = getMultipartFormParts(boundary,
            MultipartFormBuilder(boundary)
                .part("This is the content of the file\n",
                    "Content-Disposition" to listOf("form-data" to null, "name" to "fileFieldName", "filename" to "filename.txt"),
                    "Content-Type" to listOf("plain/text" to null),
                    "Some-header" to listOf("some value" to null))
                .part("This is the content of the field\n",
                    "Content-Disposition" to listOf("form-data" to null, "name" to "fieldFieldName"),
                    "Another-header" to listOf("some-key" to "some-value"))
                .build())

        val file = assertFilePart(form, "fileFieldName", "filename.txt", "plain/text", "This is the content of the file\n")

        val fileHeaders = file.headers
        assertThat(fileHeaders.size, equalTo(3))
        assertThat(fileHeaders["Content-Disposition"], equalTo("form-data; name=\"fileFieldName\"; filename=\"filename.txt\""))
        assertThat(fileHeaders["Content-Type"], equalTo("plain/text"))
        assertThat(fileHeaders["Some-header"], equalTo("some value"))

        val field = assertFieldPart(form, "fieldFieldName", "This is the content of the field\n")

        val fieldHeaders = field.headers
        assertThat(fieldHeaders.size, equalTo(2))
        assertThat(fieldHeaders["Content-Disposition"], equalTo("form-data; name=\"fieldFieldName\""))
        assertThat(fieldHeaders["Another-header"], equalTo("some-key=\"some-value\""))

        assertThereAreNoMoreParts(form)
    }

    @Test
    fun closedPartsCannotBeReadFrom() {
        val boundary = "-----2345"
        val form = getMultipartFormParts(boundary, MultipartFormBuilder(boundary)
            .file("aFile", "file.name", "application/octet-stream", "File contents here".byteInputStream()).build())

        val file = form.next()


        while (file.inputStream.read() > 0) {
            // keep reading.
        }

        assertThat(file.inputStream.read(), equalTo(-1))
        file.inputStream.close()
        file.inputStream.close() // can close multiple times
        try {
            val ignored = file.inputStream.read()
            fail("Should have complained that the StreamingPart has been closed " + ignored)
        } catch (e: AlreadyClosedException) {
            // pass
        }

    }

    @Test
    fun readingPartsContentsAsStringClosesStream() {
        val boundary = "-----2345"
        val form = getMultipartFormParts(boundary, MultipartFormBuilder(boundary)
            .file("aFile", "file.name", "application/octet-stream", "File contents here".byteInputStream()).build())

        val file = form.next()
        file.contentsAsString

        try {
            val ignored = file.inputStream.read()
            fail("Should have complained that the StreamingPart has been closed " + ignored)
        } catch (e: AlreadyClosedException) {
            // pass
        }

        file.inputStream.close() // can close multiple times
    }

    @Test
    fun gettingNextPartClosesOldPart() {
        val boundary = "-----2345"
        val form = getMultipartFormParts(boundary, MultipartFormBuilder(boundary)
            .file("aFile", "file.name", "application/octet-stream", "File contents here".byteInputStream())
            .file("anotherFile", "your.name", "application/octet-stream", "Different file contents here".byteInputStream()).build())

        val file1 = form.next()

        val file2 = form.next()

        assertThat(file1, not(equalTo(file2)))

        try {
            val ignored = file1.inputStream.read()
            fail("Should have complained that the StreamingPart has been closed " + ignored)
        } catch (e: AlreadyClosedException) {
            // pass
        }

        file1.inputStream.close() // can close multiple times

        assertThat(file2.contentsAsString, equalTo("Different file contents here"))
    }

    @Test
    fun canLoadComplexRealLifeSafariExample() {
        val parts = StreamingMultipartFormParts.parse(
            "----WebKitFormBoundary6LmirFeqsyCQRtbj".toByteArray(StandardCharsets.UTF_8),
            FileInputStream("examples/safari-example.multipart"),
            StandardCharsets.UTF_8
        ).iterator()

        assertFieldPart(parts, "articleType", "obituary")

        assertRealLifeFile(parts, "simple7bit.txt", "text/plain")
        assertRealLifeFile(parts, "starbucks.jpeg", "image/jpeg")
        assertRealLifeFile(parts, "utf8\uD83D\uDCA9.file", "application/octet-stream")
        assertRealLifeFile(parts, "utf8\uD83D\uDCA9.txt", "text/plain")

    }

    @Test
    fun canLoadComplexRealLifeChromeExample() {
        val parts = StreamingMultipartFormParts.parse(
            "----WebKitFormBoundaryft3FGhOMTYoOkCCc".toByteArray(StandardCharsets.UTF_8),
            FileInputStream("examples/chrome-example.multipart"),
            StandardCharsets.UTF_8
        ).iterator()

        assertFieldPart(parts, "articleType", "obituary")

        assertRealLifeFile(parts, "simple7bit.txt", "text/plain")
        assertRealLifeFile(parts, "starbucks.jpeg", "image/jpeg")
        assertRealLifeFile(parts, "utf8\uD83D\uDCA9.file", "application/octet-stream")
        assertRealLifeFile(parts, "utf8\uD83D\uDCA9.txt", "text/plain")

    }


}

val CR_LF = "\r\n"


internal fun assertRealLifeFile(parts: Iterator<StreamingPart>, fileName: String, contentType: String) {
    val file = parts.next()
    assertThat("field name", file.fieldName, equalTo("uploadManuscript"))
    assertThat("file name", file.fileName, equalTo(fileName))
    assertThat("content type", file.contentType, equalTo(contentType))
    assertPartIsNotField(file)
    compareStreamToFile(file)
}

internal fun compareStreamToFile(file: StreamingPart) {
    val formFile = file.inputStream
    compareStreamToFile(formFile, file.fileName)
}


fun compareStreamToFile(actualSream: InputStream, fileName: String?) {
    val original = FileInputStream("examples/" + fileName!!)
    compareOneStreamToAnother(actualSream, original)
}


fun compareOneStreamToAnother(actualStream: InputStream, expectedStream: InputStream) {
    var index = 0
    while (true) {
        val actual = actualStream.read()
        val expected = expectedStream.read()
        assertThat("index " + index, actual, equalTo(expected))
        index++
        if (actual < 0) {
            break
        }
    }
}

internal fun getMultipartFormParts(boundary: String, multipartFormContents: ByteArray): Iterator<StreamingPart> {
    return getMultipartFormParts(boundary.toByteArray(StandardCharsets.UTF_8), multipartFormContents, StandardCharsets.UTF_8)
}

internal fun getMultipartFormParts(boundary: ByteArray, multipartFormContents: ByteArray, encoding: Charset): Iterator<StreamingPart> {
    val multipartFormContentsStream = ByteArrayInputStream(multipartFormContents)
    return StreamingMultipartFormParts.parse(boundary, multipartFormContentsStream, encoding).iterator()
}

internal fun assertFilePart(form: Iterator<StreamingPart>, fieldName: String, fileName: String, contentType: String, contents: String, encoding: Charset = StandardCharsets.UTF_8): StreamingPart {
    assertThereAreMoreParts(form)
    val file = form.next()
    assertThat("file name", file.fileName, equalTo(fileName))
    assertThat("content type", file.contentType, equalTo(contentType))
    assertPartIsNotField(file)
    assertPart(fieldName, contents, file, encoding)
    return file
}

internal fun assertFieldPart(form: Iterator<StreamingPart>, fieldName: String, fieldValue: String, encoding: Charset = StandardCharsets.UTF_8): StreamingPart {
    assertThereAreMoreParts(form)
    val field = form.next()
    assertPartIsFormField(field)
    assertPart(fieldName, fieldValue, field, encoding)
    return field
}


internal fun assertPart(fieldName: String, fieldValue: String, StreamingPart: StreamingPart, encoding: Charset) {
    assertThat("field name", StreamingPart.fieldName, equalTo(fieldName))
    assertThat("contents", StreamingPart.inputStream.reader(encoding).readText(), equalTo(fieldValue))
}

internal fun assertThereAreNoMoreParts(form: Iterator<StreamingPart>) {
    assertFalse("Too many parts", form.hasNext())
}

internal fun assertThereAreMoreParts(form: Iterator<StreamingPart>) {
    assertTrue("Not enough parts", form.hasNext())
}

internal fun assertPartIsFormField(field: StreamingPart) {
    assertTrue("the StreamingPart is a form field", field.isFormField)
}

internal fun assertPartIsNotField(file: StreamingPart) {
    assertFalse("the StreamingPart is not a form field", file.isFormField)
}