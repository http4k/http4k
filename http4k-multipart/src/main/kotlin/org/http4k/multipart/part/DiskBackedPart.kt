package org.http4k.multipart.part

import java.io.File
import java.io.FileInputStream
import java.io.InputStream
import java.nio.file.FileSystemException

class DiskBackedPart(part: PartMetaData, private val theFile: File) : Part(part.fieldName, part.isFormField, part.contentType, part.fileName, part.headers, theFile.length().toInt()) {

    override val newInputStream: InputStream
        get() = FileInputStream(theFile)

    override val isInMemory = false

    override val bytes
        get() = throw IllegalStateException("Cannot get bytes from a DiskBacked Part. Check with isInMemory()")

    override val string
        get() = throw IllegalStateException("Cannot get bytes from a DiskBacked Part. Check with isInMemory()")


    override fun close() {
        if (!theFile.delete()) {
            throw FileSystemException("Failed to delete file")
        }
    }
}
