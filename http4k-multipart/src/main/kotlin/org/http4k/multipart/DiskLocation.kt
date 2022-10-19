package org.http4k.multipart

import java.io.Closeable
import java.io.File
import java.nio.file.FileSystemException
import java.nio.file.Files
import java.util.UUID

/**
 * Provides the management of the on-disk persistence for mulitpart files which are too big
 * to be processed in-memory.
 */
interface DiskLocation {
    fun createFile(filename: String?): MultipartFile

    companion object {
        fun Temp(diskDir: File = Files.createTempDirectory("http4k-mp").toFile().apply { deleteOnExit() }) =
            object : DiskLocation {
                override fun createFile(filename: String?): MultipartFile =
                    TempFile(
                        File.createTempFile(
                            filename ?: (UUID.randomUUID().toString() + "-"),
                            ".tmp", diskDir
                        ).apply {
                            deleteOnExit()
                        }
                    )
            }

        fun Permanent(diskDir: File = Files.createTempDirectory("http4k-mp").toFile()) =
            object : DiskLocation {
                override fun createFile(filename: String?): MultipartFile =
                    PermanentFile(
                        File.createTempFile(
                            filename ?: (UUID.randomUUID().toString() + "-"),
                            ".tmp", diskDir
                        )
                    )
            }
    }
}

interface MultipartFile : Closeable {
    fun file(): File
    override fun close()
}

internal class TempFile(private val file: File) : MultipartFile {

    override fun file() = file

    override fun close() {
        if (!file.delete()) throw FileSystemException("Failed to delete file")
    }
}

internal class PermanentFile(private val file: File) : MultipartFile {
    override fun file() = file

    override fun close() {
        // do nothing
    }
}
