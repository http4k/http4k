package org.http4k.multipart.part

import java.io.File
import java.io.FileInputStream
import java.io.IOException
import java.io.InputStream
import java.nio.file.FileSystemException

class DiskBackedPart(part: PartMetaData, private val theFile: File) : Part(part.fieldName, part.isFormField, part.contentType, part.fileName, part.headers, theFile.length().toInt()) {

    override val newInputStream: InputStream
        @Throws(IOException::class)
        get() = FileInputStream(theFile)

    override val isInMemory: Boolean
        get() = false

    override val bytes: ByteArray
        get() = throw IllegalStateException("Cannot get bytes from a DiskBacked Part. Check with isInMemory()")

    override val string: String
        get() = throw IllegalStateException("Cannot get bytes from a DiskBacked Part. Check with isInMemory()")

    @Throws(IOException::class)
    override fun close() {
        if (!theFile.delete()) {
            throw FileSystemException("Failed to delete file")
        }
    }
}
