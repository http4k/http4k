package org.http4k.testing

import java.io.File
import java.io.InputStream
import java.io.OutputStream

fun interface ReadResource {
    fun input(): InputStream?
}

interface ReadWriteResource : ReadResource {
    fun output(): OutputStream
}

/**
 * The backing store where the approved and actual content will be stored.
 */
interface ApprovalSource {
    fun actualFor(testName: String): ReadWriteResource
    fun approvedFor(testName: String): ReadResource
}

class FileSystemApprovalSource(private val base: File) : ApprovalSource {
    override fun actualFor(testName: String): ReadWriteResource = FileReadWriteResource(File(base, "$testName.actual"))

    override fun approvedFor(testName: String): ReadResource = FileReadWriteResource(File(base, "$testName.approved"))
}

internal class FileReadWriteResource(private val target: File) : ReadWriteResource {
    override fun input() = if (target.exists()) target.inputStream() else null

    override fun output(): OutputStream =
        with(target) {
            if (!parentFile.exists() && !parentFile.mkdirs()) throw IllegalAccessException("Could not create dir ${parentFile.absolutePath}")
            if (exists() && !delete()) throw IllegalAccessException("Could not delete $absolutePath")
            outputStream()
        }

    override fun toString(): String = target.absolutePath
}
