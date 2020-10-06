package org.http4k.multipart

import org.http4k.multipart.Part.DiskBacked
import org.http4k.multipart.Part.InMemory
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.nio.charset.Charset
import java.util.UUID

/**
 * Parser for creating serialised form parts using the encoding
 * and maximum Part size specified. If a Part is bigger than the writeToDiskThreshold, then it will be written to
 * disk in temporaryFileDirectory (or the default temp dir if null).
 *
 * @param encoding               encoding of the stream
 * @param writeToDiskThreshold   if a Part is bigger than this threshold it will be purged from memory
 * and written to disk
 * @param temporaryFileDirectory where to write the files for Parts that are too big. Uses the default
 * temporary directory if null.
 */
internal class MultipartFormParser(private val encoding: Charset, private val writeToDiskThreshold: Int, private val temporaryFileDirectory: File) {

    /**
     * Returns a list of Parts.
     *
     * To limit the overall size of the stream, pass the appropriate parameter to StreamingMultipartFormParts
     *
     * The Parts object must be closed when finished with so that the files that have been written to disk can be
     * deleted.
     *
     * @param parts                  streaming parts
     * @return Parts object, which contains the Map of Fieldname to List of Parts. This object must
     * be closed so that it is cleaned up after.
     * @throws IOException
     */

    fun formParts(parts: Iterable<StreamingPart>): List<Part> {
        val result = mutableListOf<Part>()
        val bytes = ByteArray(writeToDiskThreshold)

        for (part in parts) {
            if (part.fieldName == null) throw ParseError("no name for part")

            result.add(serialisePart(part, bytes))
        }
        return result
    }

    private fun serialisePart(part: StreamingPart, bytes: ByteArray): Part {
        var length = 0

        while (true) {
            val count = part.inputStream.read(bytes, length, writeToDiskThreshold - length)
            if (count < 0) {
                part.inputStream.use {
                    return InMemory(
                        part,
                        storeInMemory(bytes, length), encoding
                    )
                }
            }
            length += count
            if (length >= writeToDiskThreshold) {
                part.inputStream.use {
                    return DiskBacked(
                        part,
                        writeToDisk(part, bytes, length)
                    )
                }
            }
        }
    }

    private fun storeInMemory(bytes: ByteArray, length: Int) = ByteArray(length).apply { System.arraycopy(bytes, 0, this, 0, length) }

    private fun writeToDisk(part: StreamingPart, bytes: ByteArray, length: Int) =
        File.createTempFile(
            part.fileName ?: UUID.randomUUID().toString() +
                "-",
            ".tmp", temporaryFileDirectory
        ).apply {
            deleteOnExit()
            FileOutputStream(this).apply {
                write(bytes, 0, length)
                use { part.inputStream.copyTo(it, writeToDiskThreshold) }
            }
        }
}
