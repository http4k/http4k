package org.http4k.multipart

import org.http4k.multipart.exceptions.ParseError
import org.http4k.multipart.part.Part
import org.http4k.multipart.part.Part.DiskBacked
import org.http4k.multipart.part.Part.InMemory
import org.http4k.multipart.part.Parts
import org.http4k.multipart.part.StreamingPart
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.io.InputStream
import java.nio.charset.Charset
import java.util.*

object MultipartFormMap {

    /**
     * Returns a Parts object containing a map of FieldName -> Part, serialised from parts using the encoding
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

    fun formMap(parts: Iterable<StreamingPart>, encoding: Charset, writeToDiskThreshold: Int, temporaryFileDirectory: File): Parts {
        val partMap = HashMap<String, List<Part>>()
        val bytes = ByteArray(writeToDiskThreshold)

        for (part in parts) {
            if (part.fieldName == null) throw ParseError("no name for part")

            val keyParts = (partMap[part.fieldName] ?: listOf()).let {
                it + (serialisePart(encoding, writeToDiskThreshold, temporaryFileDirectory, part, part.inputStream, bytes))
            }
            partMap.put(part.fieldName, keyParts)
        }
        return Parts(partMap)
    }


    private fun serialisePart(encoding: Charset, writeToDiskThreshold: Int, temporaryFileDirectory: File, part: StreamingPart, partInputStream: InputStream, bytes: ByteArray): Part {
        var length = 0

        while (true) {
            val count = partInputStream.read(bytes, length, writeToDiskThreshold - length)
            if (count < 0) {
                return InMemory(
                    part,
                    storeInMemory(bytes, length, partInputStream), encoding)
            }
            length += count
            if (length >= writeToDiskThreshold) {
                return DiskBacked(
                    part,
                    writeToDisk(part.fileName, writeToDiskThreshold, temporaryFileDirectory, bytes, length, partInputStream))
            }
        }
    }

    private fun storeInMemory(bytes: ByteArray, length: Int, partInputStream: InputStream): ByteArray {
        partInputStream.close()

        val result = ByteArray(length)
        System.arraycopy(bytes, 0, result, 0, length)
        return result
    }


    private fun writeToDisk(fileName: String?, writeToDiskThreshold: Int, temporaryFileDirectory: File, bytes: ByteArray, length: Int, partInputStream: InputStream): File {
        val tempFile = File.createTempFile(fileName ?: UUID.randomUUID().toString() + "-", ".tmp", temporaryFileDirectory)
        tempFile.deleteOnExit()
        val outputStream = FileOutputStream(tempFile)
        outputStream.write(bytes, 0, length)
        while (true) {
            val readLength = partInputStream.read(bytes, 0, writeToDiskThreshold)
            if (readLength < 0) {
                break
            }
            outputStream.write(bytes, 0, readLength)
        }
        partInputStream.close()
        return tempFile
    }
}
