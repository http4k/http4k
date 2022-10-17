package org.http4k.multipart

import com.natpryce.hamkrest.assertion.assertThat
import com.natpryce.hamkrest.containsSubstring
import com.natpryce.hamkrest.equalTo
import org.http4k.core.string
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.fail
import java.io.ByteArrayInputStream
import java.io.File
import java.io.FileInputStream
import java.io.IOException
import java.io.InputStream
import java.nio.charset.StandardCharsets.ISO_8859_1
import java.nio.charset.StandardCharsets.UTF_8
import java.util.Arrays

class MultipartFormParserTest {

    @Test
    @Disabled
    fun example() {

        val maxStreamLength = 100000 // maximum length of the stream, will throw exception if this is exceeded
        val writeToDiskThreshold = 1024 // maximum length of in memory object - if part is bigger then write to disk
        val temporaryFileDirectory: File? = null // use default temporary file directory
        val contentType = "multipart/form-data; boundary=------WebKitFormBoundary6LmirFeqsyCQRtbj" // content type from HTTP header

        // you are responsible for closing the body InputStream
        try {
            FileInputStream("examples/safari-example.multipart").use { body ->

                val boundary = contentType.substring(contentType.indexOf("boundary=") + "boundary=".length).toByteArray(ISO_8859_1)
                val streamingParts = StreamingMultipartFormParts.parse(
                    boundary, body, ISO_8859_1, maxStreamLength
                )

                val parts = MultipartFormParser(UTF_8, writeToDiskThreshold, TempDiskLocation(TEMPORARY_FILE_DIRECTORY)).formParts(streamingParts)
                parts.let {
                    val articleType = it.parts("articleType")[0]
                    println(articleType.fieldName) // "articleType"
                    println(articleType.headers) // {Content-Disposition=form-data; name="articleType"}
                    println(articleType.length) // 8 bytes
                    println(articleType.isInMemory()) // true
                    println(articleType.string()) // "obituary"

                    val simple7bit = it.parts("uploadManuscript")[0]
                    println(simple7bit.fieldName) // "uploadManuscript"
                    println(simple7bit.fileName) // "simple7bit.txt"
                    println(simple7bit.headers) // {Content-Disposition => form-data; name="uploadManuscript"; filename="simple7bit.txt"
                    // Content-Type => text/plain}
                    println(simple7bit.length) // 8221 bytes
                    println(simple7bit.isInMemory()) // false
                    simple7bit.newInputStream // stream of the contents of the file
                }
            }
        } catch (e: IOException) {
            // general stream exceptions
        }
    }

    @Test
    fun uploadMultipleFilesAndFields() {
        val boundary = "-----1234"
        val multipartFormContentsStream = MultipartFormBuilder(boundary)
            .file("file", "foo.tab", "text/whatever", "This is the content of the file\n".byteInputStream(), emptyList())
            .field("field", "fieldValue" + CR_LF + "with cr lf", emptyList())
            .field("multi", "value1", emptyList())
            .file("anotherFile", "BAR.tab", "text/something", "This is another file\n".byteInputStream(), emptyList())
            .field("multi", "value2", emptyList())
            .stream()
        val form = StreamingMultipartFormParts.parse(boundary.toByteArray(UTF_8), multipartFormContentsStream, UTF_8)

        val parts = MultipartFormParser(UTF_8, 1024, TempDiskLocation(TEMPORARY_FILE_DIRECTORY)).formParts(form)

        assertThat(parts.parts("file")[0].fileName, equalTo("foo.tab"))
        assertThat(parts.parts("anotherFile")[0].fileName, equalTo("BAR.tab"))
        compareOneStreamToAnother(parts.parts("field")[0].newInputStream, ByteArrayInputStream(("fieldValue" + CR_LF + "with cr lf").toByteArray()))
        compareOneStreamToAnother(parts.parts("multi")[0].newInputStream, ByteArrayInputStream("value1".toByteArray()))
        compareOneStreamToAnother(parts.parts("multi")[1].newInputStream, ByteArrayInputStream("value2".toByteArray()))
        parts.close()
    }

    @Test
    fun canLoadComplexRealLifeSafariExample() {
        val form = safariExample()

        val parts = MultipartFormParser(UTF_8, 1024000, TempDiskLocation(TEMPORARY_FILE_DIRECTORY)).formParts(form)
        allFieldsAreLoadedCorrectly(parts, true, true, true, true)
        parts.forEach { it.close() }
    }

    @Test
    fun throwsExceptionIfFormIsTooBig() {
        val form = StreamingMultipartFormParts.parse(
            "----WebKitFormBoundary6LmirFeqsyCQRtbj".toByteArray(UTF_8),
            FileInputStream("examples/safari-example.multipart"),
            UTF_8,
            1024
        )

        try {
            MultipartFormParser(UTF_8, 1024, TempDiskLocation(TEMPORARY_FILE_DIRECTORY)).formParts(form)
            fail("should have failed because the form is too big")
        } catch (e: Throwable) {
            assertThat(e.localizedMessage, containsSubstring("Form contents was longer than 1024 bytes"))
        }
    }

    @Test
    fun savesAllPartsToDisk() {
        val form = safariExample()

        val parts = MultipartFormParser(UTF_8, 100, TempDiskLocation(TEMPORARY_FILE_DIRECTORY)).formParts(form)

        allFieldsAreLoadedCorrectly(parts, false, false, false, false)

        assertThat(temporaryFileList()!!.size, equalTo(4))
        parts.close()
        assertThat(temporaryFileList()!!.size, equalTo(0))
    }

    @Test
    fun savesSomePartsToDisk() {
        val form = safariExample()

        val parts = MultipartFormParser(UTF_8, 1024 * 4, TempDiskLocation(TEMPORARY_FILE_DIRECTORY)).formParts(form)

        allFieldsAreLoadedCorrectly(parts, false, true, true, false)

        val files = temporaryFileList()
        assertPartSaved("simple7bit.txt", files)
        assertPartSaved("starbucks.jpeg", files)
        assertThat(files!!.size, equalTo(2))

        parts.close()
        assertThat(temporaryFileList()!!.size, equalTo(0))
    }

    @Test
    fun shouldKeepSomePartsOnDisk() {
        try {
            val form = safariExample()

            val parts = MultipartFormParser(UTF_8, 1024 * 4, PermanentDiskLocation(TEMPORARY_FILE_DIRECTORY)).formParts(form)

            allFieldsAreLoadedCorrectly(parts, false, true, true, false)

            val files = temporaryFileList()
            assertPartSaved("simple7bit.txt", files)
            assertPartSaved("starbucks.jpeg", files)
            assertThat(files!!.size, equalTo(2))

            parts.close()
            assertThat(temporaryFileList()!!.size, equalTo(2))
        } finally {
            temporaryFileList()!!.forEach { TEMPORARY_FILE_DIRECTORY.resolve(it).delete() }
        }
    }

    @Test
    fun throwsExceptionIfMultipartMalformed() {
        val form = StreamingMultipartFormParts.parse(
            "---2345".toByteArray(UTF_8),
            ByteArrayInputStream(
                ("-----2345" + CR_LF +
                    "Content-Disposition: form-data; name=\"name\"" + CR_LF +
                    "" + CR_LF +
                    "value" + // no CR_LF

                    "-----2345--" + CR_LF).toByteArray()
            ),
            UTF_8
        )

        try {
            MultipartFormParser(UTF_8, 1024 * 4, TempDiskLocation(TEMPORARY_FILE_DIRECTORY)).formParts(form)
            fail("Should have thrown an Exception")
        } catch (e: Throwable) {
            assertThat(e.localizedMessage, containsSubstring("Boundary must be proceeded by field separator, but didn't find it"))
        }
    }

    private fun safariExample(): Iterable<StreamingPart> = StreamingMultipartFormParts.parse(
        "----WebKitFormBoundary6LmirFeqsyCQRtbj".toByteArray(UTF_8),
        FileInputStream("examples/safari-example.multipart"),
        UTF_8
    )

    private fun allFieldsAreLoadedCorrectly(partMap: List<Part>, simple7bit: Boolean, file: Boolean, txt: Boolean, jpeg: Boolean) {
        assertFileIsCorrect(partMap.filter { it.fieldName == "uploadManuscript" }[0], "simple7bit.txt", simple7bit)
        assertFileIsCorrect(partMap.filter { it.fieldName == "uploadManuscript" }[2], "utf8\uD83D\uDCA9.file", file)

        val articleType = partMap.filter { it.fieldName == "articleType" }[0]
        assertThat("articleType", articleType.isInMemory(), equalTo(true))
        assertThat(articleType.string(), equalTo("obituary"))

        assertFileIsCorrect(partMap.filter { it.fieldName == "uploadManuscript" }[3], "utf8\uD83D\uDCA9.txt", txt)
        assertFileIsCorrect(partMap.filter { it.fieldName == "uploadManuscript" }[1], "starbucks.jpeg", jpeg)
    }

    private fun assertPartSaved(fileName: String, files: Array<String>?) {
        assertThat(
            "couldn't find " + fileName + " in " + Arrays.toString(files),
            files!![0].contains(fileName) || files[1].contains(fileName), equalTo(true)
        )
    }

    private fun assertFileIsCorrect(filePart: Part, expectedFilename: String, inMemory: Boolean) {
        assertFileIsCorrect(filePart, expectedFilename, filePart.newInputStream, inMemory)
    }

    private fun assertFileIsCorrect(filePart: Part, expectedFilename: String, inputStream: InputStream, inMemory: Boolean) {
        assertThat("$expectedFilename in memory?", filePart.isInMemory(), equalTo(inMemory))
        assertThat(filePart.fileName, equalTo(expectedFilename))
        compareStreamToFile(inputStream, filePart.fileName)
    }

    private fun temporaryFileList(): Array<String>? = TEMPORARY_FILE_DIRECTORY.list()

    companion object {
        val TEMPORARY_FILE_DIRECTORY = File("./out/tmp")

        init {
            TEMPORARY_FILE_DIRECTORY.mkdirs()
        }
    }
}

private fun List<Part>.close(): Unit = forEach(Part::close)

private fun List<Part>.parts(name: String): List<Part> = filter { it.fieldName == name }

internal fun Part.isInMemory(): Boolean = this is Part.InMemory
