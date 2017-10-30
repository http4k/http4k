package org.http4k.multipart

import org.http4k.multipart.Part.DiskBacked
import org.http4k.multipart.Part.InMemory
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.io.InputStream
import java.nio.charset.Charset
import java.util.*

internal object MultipartFormMap {

    /**
     * Returns a list of Parts, serialised from parts using the encoding
     * and maximum Part size specified. If a Part is bigger than the writeToDiskThreshold, then it will be written to
     * disk in temporaryFileDirectory (or the default temp dir if null).
     *
     *
     * To limit the overall size of the stream, pass the appropriate parameter to StreamingMultipartFormParts
     *
     *
     * The Parts object must be closed when finished with so that the files that have been written to disk can be
     * deleted.
     *
     * @param parts                  streaming parts
     * @param encoding               encoding of the stream
     * @param writeToDiskThreshold   if a Part is bigger than this threshold it will be purged from memory
     * and written to disk
     * @param temporaryFileDirectory where to write the files for Parts that are too big. Uses the default
     * temporary directory if null.
     * @return Parts object, which contains the Map of Fieldname to List of Parts. This object must
     * be closed so that it is cleaned up after.
     * @throws IOException
     */

    fun formParts(parts: Iterable<StreamingPart>, encoding: Charset, writeToDiskThreshold: Int, temporaryFileDirectory: File): List<Part> {
        val result = mutableListOf<Part>()
        val bytes = ByteArray(writeToDiskThreshold)

        for (part in parts) {
            if (part.fieldName == null) throw ParseError("no name for part")

            result.add(serialisePart(encoding, writeToDiskThreshold, temporaryFileDirectory, part, bytes))
        }
        return result
    }

    private fun serialisePart(encoding: Charset, writeToDiskThreshold: Int, temporaryFileDirectory: File, part: StreamingPart, bytes: ByteArray): Part {
        var length = 0

        while (true) {
            val count = part.inputStream.read(bytes, length, writeToDiskThreshold - length)
            if (count < 0) {
                return InMemory(
                    part,
                    storeInMemory(bytes, length, part.inputStream), encoding)
            }
            length += count
            if (length >= writeToDiskThreshold) {
                return DiskBacked(
                    part,
                    writeToDisk(part, writeToDiskThreshold, temporaryFileDirectory, bytes, length))
            }
        }
    }

    private fun storeInMemory(bytes: ByteArray, length: Int, partInputStream: InputStream): ByteArray {
        partInputStream.close()

        val result = ByteArray(length)
        System.arraycopy(bytes, 0, result, 0, length)
        return result
    }


    private fun writeToDisk(part: StreamingPart, writeToDiskThreshold: Int, temporaryFileDirectory: File, bytes: ByteArray, length: Int): File {
        val tempFile = File.createTempFile(part.fileName ?: UUID.randomUUID().toString() + "-", ".tmp", temporaryFileDirectory)
        tempFile.deleteOnExit()
        val outputStream = FileOutputStream(tempFile)
        outputStream.write(bytes, 0, length)
        while (true) {
            val readLength = part.inputStream.read(bytes, 0, writeToDiskThreshold)
            if (readLength < 0) {
                break
            }
            outputStream.write(bytes, 0, readLength)
        }
        part.inputStream.close()
        return tempFile
    }
}
