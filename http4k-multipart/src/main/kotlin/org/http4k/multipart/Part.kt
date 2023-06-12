package org.http4k.multipart

import java.io.ByteArrayInputStream
import java.io.Closeable
import java.io.FileInputStream
import java.io.InputStream
import java.nio.charset.Charset

internal sealed class Part(fieldName: String?, formField: Boolean, contentType: String?, fileName: String?, headers: Map<String, String>, val length: Int) :
    PartMetaData(fieldName, formField, contentType, fileName, headers), Closeable {

    abstract val newInputStream: InputStream

    abstract val bytes: ByteArray

    class DiskBacked(part: PartMetaData, private val multipartFile: MultipartFile) : Part(part.fieldName, part.isFormField, part.contentType, part.fileName, part.headers, multipartFile.file().length().toInt()) {
        override val newInputStream: InputStream
            get() = FileInputStream(multipartFile.file())

        override val bytes
            get() = throw IllegalStateException("Cannot get bytes from a DiskBacked Part")

        override fun close() {
            multipartFile.close()
        }
    }

    class InMemory(
        original: PartMetaData,
        override val bytes: ByteArray /* not immutable*/,
        internal val encoding: Charset
    ) : Part(original.fieldName, original.isFormField, original.contentType, original.fileName, original.headers, bytes.size) {

        override val newInputStream: InputStream
            get() = ByteArrayInputStream(bytes)

        override fun close() {
            // do nothing
        }
    }
}
