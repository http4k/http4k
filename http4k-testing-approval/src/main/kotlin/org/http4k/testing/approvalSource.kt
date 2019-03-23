package org.http4k.testing

import java.io.File
import java.io.InputStream
import java.io.OutputStream

interface ReadResource {
    fun input(): InputStream?
}

interface ReadWriteResource : ReadResource {
    fun output(): OutputStream
}

interface ApprovalSource {
    fun actualFor(testName: String): ReadWriteResource
    fun approvedFor(testName: String): ReadResource
}

class FileSystemApprovalSource(private val base: File) : ApprovalSource {
    override fun actualFor(testName: String) = with(File(base, "$testName.actual")) {
        object : ReadWriteResource {
            override fun input() = if (exists()) inputStream() else null

            override fun output(): OutputStream {
                if (exists()) delete()
                if (!parentFile.mkdirs()) throw IllegalAccessException("Could not create ${parentFile.absolutePath}")
                return outputStream()
            }

            override fun toString() = absolutePath
        }
    }

    override fun approvedFor(testName: String): ReadResource =
        with(File(base, "$testName.approved")) {
            object : ReadResource {
                override fun input() = if (exists()) inputStream() else null
                override fun toString() = absolutePath
            }
        }
}